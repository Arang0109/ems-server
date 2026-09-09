package com.ensolution.ems.chat.presentation.room.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대화 상대")
public record ChatRoomPeerResponse(
	Long userId,
	String name,
	String department,
	@Schema(description = "지금 접속 중인가. 살아 있는 WebSocket 세션이 기준이라 "
		+ "브라우저를 강제 종료해도 90초 안에 false 가 된다")
	boolean online
) {
}
