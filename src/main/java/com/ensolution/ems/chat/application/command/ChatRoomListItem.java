package com.ensolution.ems.chat.application.command;

import java.time.LocalDateTime;

/**
 * 대화방 목록 한 줄.
 * <p>
 * {@code unreadCount}는 저장된 값이 아니라 조회 시점에 센 값입니다
 * ({@code ChatRoomListAssembler} 참고).
 */
public record ChatRoomListItem(
	Long roomId,
	ChatRoomPeer peer,
	String lastMessageId,
	String lastMessagePreview,
	LocalDateTime lastMessageAt,
	long unreadCount
) {
}
