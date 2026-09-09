package com.ensolution.ems.chat.application.service.support;

import com.ensolution.ems.chat.application.event.ChatMessagePayload;
import com.ensolution.ems.chat.application.event.ChatPresencePayload;
import com.ensolution.ems.chat.application.event.ChatReadPayload;
import com.ensolution.ems.chat.application.event.ChatRoomOpenedPayload;
import com.ensolution.ems.chat.application.port.out.ChatEventBroadcaster;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * 실시간 알림을 <b>커밋 이후에</b> 발행하는 정책을 캡슐화합니다.
 * <p>
 * 서비스가 브로드캐스터를 직접 부르면 뒤이어 롤백될 저장까지 "저장됐다"고 알리게 됩니다.
 * 채팅에서는 그 결과가 특히 나쁩니다 — 상대 화면에 뜬 말풍선을 되돌릴 방법이 없고, 서버를
 * 다시 조회해도 그 메시지는 없습니다. 반대로 알림 전송 실패가 저장을 되돌려서도 안 됩니다.
 * <p>
 * 두 서비스({@code ChatMessageService}·{@code ChatRoomService})가 같은 정책을 쓰므로 협력자로
 * 뽑았습니다. {@code schedule}은 같은 처리를 서비스의 private 메서드로 두고 있는데, 그쪽은
 * 발행 지점이 하나뿐이라 그렇습니다.
 * <p>
 * 트랜잭션 밖에서 불린 경우(테스트 등 동기화 비활성)에는 즉시 발행합니다.
 *
 * <p><b>접미사 주석</b>: 루트 {@code CLAUDE.md}의 협력자 표에 {@code ~Publisher}는 없습니다.
 * 하는 일이 조립({@code Assembler})도 탐색({@code Finder})도 동시 쓰기 정책({@code Writer})도
 * 아니라 <b>트랜잭션 경계 정책</b>이라 기존 접미사 어디에도 맞지 않아 새로 둡니다.
 */
@Component
@RequiredArgsConstructor
public class ChatEventPublisher {

	private final ChatEventBroadcaster broadcaster;

	public void messageCreated(List<String> recipientUsernames, ChatMessagePayload payload) {
		afterCommit(() -> broadcaster.publishMessageCreated(recipientUsernames, payload));
	}

	public void messagesRead(String recipientUsername, ChatReadPayload payload) {
		afterCommit(() -> broadcaster.publishMessagesRead(recipientUsername, payload));
	}

	public void roomOpened(String recipientUsername, ChatRoomOpenedPayload payload) {
		afterCommit(() -> broadcaster.publishRoomOpened(recipientUsername, payload));
	}

	/**
	 * 프레즌스는 트랜잭션 밖(세션 이벤트)에서 불리는 것이 정상이라 대개 즉시 발행된다.
	 * 같은 경로를 쓰는 이유는 발행 지점을 한 곳에 모으기 위함이다.
	 */
	public void presenceChanged(List<String> recipientUsernames, ChatPresencePayload payload) {
		afterCommit(() -> broadcaster.publishPresenceChanged(recipientUsernames, payload));
	}

	private void afterCommit(Runnable publish) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			publish.run();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				publish.run();
			}
		});
	}
}
