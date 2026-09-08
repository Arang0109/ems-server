package com.ensolution.ems.chat.presentation.message.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "대화 이력 한 페이지")
public record ChatMessagePageResponse(
	@Schema(description = "최신순")
	List<ChatMessageResponse> messages,
	@Schema(description = "다음 페이지를 요청할 때 before 로 넘길 값. 더 없으면 null")
	String nextCursor,
	boolean hasMore
) {
}
