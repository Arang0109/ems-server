package com.ensolution.ems.chat.presentation.room.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대화방 단건")
public record ChatRoomResponse(
	Long roomId,
	ChatRoomPeerResponse peer,
	@Schema(description = "내가 마지막으로 읽은 메시지 id. 방을 열 때 이 위치부터 보여 준다")
	String lastReadMessageId
) {
}
