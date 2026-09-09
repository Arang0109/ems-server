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
	long unreadCount,

	@Schema(description = "**내가** 마지막으로 읽은 메시지 id. 방에 들어갈 때 이 위치로 점프한다")
	String myLastReadMessageId,

	@Schema(description = "**상대가** 마지막으로 읽은 메시지 id. 이 id 이하인 내 메시지에 '읽음'을 붙인다")
	String peerLastReadMessageId
) {
}
