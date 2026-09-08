package com.ensolution.ems.chat.presentation.room.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "대화방 목록 한 줄")
public record ChatRoomListResponse(
	Long roomId,
	ChatRoomPeerResponse peer,
	String lastMessageId,
	String lastMessagePreview,
	LocalDateTime lastMessageAt,
	long unreadCount
) {
}
