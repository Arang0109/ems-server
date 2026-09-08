package com.ensolution.ems.chat.presentation.room.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대화 상대")
public record ChatRoomPeerResponse(
	Long userId,
	String name,
	String department,
	@Schema(description = "접속 여부. 프레즌스가 붙기 전까지는 항상 false")
	boolean online
) {
}
