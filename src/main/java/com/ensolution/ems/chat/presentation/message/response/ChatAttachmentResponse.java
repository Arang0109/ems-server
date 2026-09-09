package com.ensolution.ems.chat.presentation.message.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "첨부 파일. 실물은 다운로드 엔드포인트로 받는다")
public record ChatAttachmentResponse(
	String filename,
	String contentType,
	Long size
) {
}
