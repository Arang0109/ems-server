package com.ensolution.ems.chat.application.port.out;

import com.ensolution.ems.chat.application.event.ChatMessagePayload;
import com.ensolution.ems.chat.application.event.ChatReadPayload;
import com.ensolution.ems.chat.application.event.ChatRoomOpenedPayload;

import java.util.List;

/**
 * 실시간 알림을 수신자에게 전달하는 아웃바운드 포트.
 * <p>
 * <b>시그니처에 전송 기술이 드러나지 않습니다.</b> SSE 포트({@code ScheduleEventBroadcaster})는
 * 구독 등록에 {@code SseEmitter}를 넘겨야 해 spring-web 타입이 노출됐지만, STOMP 는 구독이
 * 브로커에서 일어나므로 이 포트는 "누구에게 무엇을"만 알면 됩니다.
 * <p>
 * 수신자는 <b>username</b>입니다. Spring 의 user destination 이 {@code Principal.getName()}으로
 * 라우팅하고, 이 프로젝트의 principal 이름이 username 이기 때문입니다. username 은 전 테넌트
 * 전역 유일이라({@code auth}의 {@code existsByUsername}에 tenant 가 없는 이유) 충돌이 없습니다.
 * <p>
 * <b>스케일아웃 교체 지점입니다.</b> 지금 구현({@code StompChatEventBroadcaster})은 이 프로세스의
 * 브로커에만 보내므로 인스턴스를 늘리면 다른 인스턴스에 붙은 사용자에게 알림이 가지 않습니다.
 * 그때는 이 포트를 구현하는 Redis 릴레이를 끼우면 되고, <b>서비스 코드는 바뀌지 않습니다</b>.
 */
public interface ChatEventBroadcaster {

	/** 방 참가자 전원에게. 발신자 자신도 포함한다 — 다른 탭·기기의 화면을 맞춰야 한다. */
	void publishMessageCreated(List<String> recipientUsernames, ChatMessagePayload payload);

	/** 읽음을 알아야 할 상대 한 사람에게. */
	void publishMessagesRead(String recipientUsername, ChatReadPayload payload);

	/** 방이 열렸다는 사실을 상대 한 사람에게. */
	void publishRoomOpened(String recipientUsername, ChatRoomOpenedPayload payload);
}
