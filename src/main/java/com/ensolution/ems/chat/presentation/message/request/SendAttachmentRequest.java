package com.ensolution.ems.chat.presentation.message.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 첨부 전송의 텍스트 부분. 파일은 {@code @RequestPart file} 로 따로 온다.
 * <p>
 * 본문은 <b>캡션</b>이라 없어도 된다 — 파일만 보내는 것이 흔한 사용이다.
 */
public record SendAttachmentRequest(
	@Schema(description = "파일과 함께 보낼 설명. 없어도 된다")
	@Size(max = 4000, message = "메시지는 4000자를 넘을 수 없습니다.")
	String content,

	@Size(max = 64)
	String clientMessageId
) {
}
