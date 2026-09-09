package com.ensolution.ems.chat.infrastructure.stomp;

import com.ensolution.ems.chat.application.event.ChatMessagePayload;
import com.ensolution.ems.chat.application.event.ChatPresencePayload;
import com.ensolution.ems.chat.application.event.ChatReadPayload;
import com.ensolution.ems.chat.application.event.ChatRoomOpenedPayload;
import com.ensolution.ems.chat.application.port.out.ChatEventBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * STOMP 로 채팅 알림을 전달하는 어댑터.
 *
 * <h2>목적지를 전부 사용자별 큐로 두는 이유</h2>
 * {@code /topic/chat.room.{roomId}} 같은 공개 목적지를 열었다면 <b>아무나 SUBSCRIBE 할 수 있어</b>
 * roomId 를 바꿔가며 남의 대화를 받아볼 수 있고, 그것을 막으려면 구독마다 참가자인지 확인하는
 * 인터셉터가 필요합니다. {@code /user/queue/...}는 Spring 이 세션 principal 로 목적지를 치환하므로
 * <b>남의 것을 구독할 방법이 애초에 없습니다</b> — 권한 검증이 "발신 시점에 참가자를 읽는 조회 1회"로
 * 끝나고, tenant 격리도 그 조회가 {@code (roomId, tenantId)}이므로 함께 보장됩니다.
 * 그래서 {@code WebSocketConfig}는 {@code /topic}을 아예 열지 않습니다.
 *
 * <h2>단일 인스턴스 전제</h2>
 * {@link SimpMessagingTemplate}이 보내는 곳은 <b>이 프로세스의 브로커</b>입니다. 서버를 여러 대로
 * 늘리면 다른 인스턴스에 붙은 사용자에게는 알림이 가지 않습니다({@code SseScheduleEventBroadcaster}가
 * 안고 있는 것과 같은 한계입니다). 스케일아웃 시점에는 {@code ChatEventBroadcaster}를 구현하는
 * Redis 릴레이를 끼우고 이 어댑터는 릴레이가 각 인스턴스에서 재발행할 때 쓰이게 됩니다 —
 * 포트가 전송 기술을 노출하지 않으므로 서비스 코드는 바뀌지 않습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompChatEventBroadcaster implements ChatEventBroadcaster {

	/** {@code /user} 접두어는 Spring 이 붙인다. 여기서는 큐 이름만 준다. */
	private static final String MESSAGES = "/queue/chat.messages";
	private static final String READS = "/queue/chat.reads";
	private static final String ROOMS = "/queue/chat.rooms";
	private static final String PRESENCE = "/queue/chat.presence";

	private final SimpMessagingTemplate messagingTemplate;

	@Override
	public void publishMessageCreated(List<String> recipientUsernames, ChatMessagePayload payload) {
		for (String username : recipientUsernames) {
			send(username, MESSAGES, payload);
		}
	}

	@Override
	public void publishMessagesRead(String recipientUsername, ChatReadPayload payload) {
		send(recipientUsername, READS, payload);
	}

	@Override
	public void publishRoomOpened(String recipientUsername, ChatRoomOpenedPayload payload) {
		send(recipientUsername, ROOMS, payload);
	}

	@Override
	public void publishPresenceChanged(List<String> recipientUsernames, ChatPresencePayload payload) {
		for (String username : recipientUsernames) {
			send(username, PRESENCE, payload);
		}
	}

	/**
	 * 접속해 있지 않은 사용자에게 보내는 것은 정상 경로다 — 브로커가 조용히 버린다.
	 * <p>
	 * 전송 실패로 예외가 올라가면 커밋이 끝난 뒤의 알림 때문에 요청 스레드가 죽는다. 알림 유실은
	 * 회복 가능하다 — 클라이언트가 재연결 후 REST 로 메꾸며, <b>조회 결과가 늘 진실의 원천</b>이다.
	 */
	private void send(String username, String destination, Object payload) {
		if (username == null) {
			return;
		}
		try {
			messagingTemplate.convertAndSendToUser(username, destination, payload);
		} catch (RuntimeException e) {
			log.warn("[CHAT] 실시간 알림 전송에 실패했습니다. username={}, destination={}, cause={}",
				username, destination, e.getMessage());
		}
	}
}
