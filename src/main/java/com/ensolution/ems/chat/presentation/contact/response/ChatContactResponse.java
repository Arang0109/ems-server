package com.ensolution.ems.chat.presentation.contact.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대화를 시작할 수 있는 사람")
public record ChatContactResponse(
	Long userId,
	String name,
	String department,
	String role,
	@Schema(description = "지금 접속 중인가")
	boolean online
) {
}
