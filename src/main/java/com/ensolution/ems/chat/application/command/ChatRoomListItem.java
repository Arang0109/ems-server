package com.ensolution.ems.chat.application.command;

import java.time.LocalDateTime;

/**
 * 대화방 목록 한 줄.
 *
 * <p>{@code unreadCount}는 저장된 값이 아니라 조회 시점에 센 값입니다
 * ({@code ChatRoomListAssembler} 참고).
 *
 * <p>읽음 커서 둘도 함께 싣습니다. 값은 이미 목록 조회가 읽어 온 참가자 행에 들어 있어
 * <b>추가 조회가 없고</b>, 없으면 방을 누를 때마다 단건 조회를 한 번 더 해야 합니다 —
 * "안 읽은 첫 메시지로 점프"와 "읽음 표시"가 둘 다 이 값에 기댑니다.
 */
public record ChatRoomListItem(
	Long roomId,
	ChatRoomPeer peer,
	String lastMessageId,
	String lastMessagePreview,
	LocalDateTime lastMessageAt,
	long unreadCount,
	String myLastReadMessageId,
	String peerLastReadMessageId
) {
}
