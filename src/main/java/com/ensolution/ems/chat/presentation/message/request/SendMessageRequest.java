package com.ensolution.ems.chat.presentation.message.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 보내는 사람과 방은 각각 인증 주체와 경로에서 온다. 본문만 받는다.
 */
public record SendMessageRequest(
	@Schema(description = "메시지 본문", example = "3번 굴뚝 채취 끝났습니다")
	@NotBlank(message = "메시지 내용을 입력해 주세요.")
	@Size(max = 4000, message = "메시지는 4000자를 넘을 수 없습니다.")
	String content,

	@Schema(description = "클라이언트가 만든 UUID. 응답과 실시간 알림에 그대로 실려 돌아오므로 "
		+ "전송 중 띄워 둔 임시 말풍선을 이 값으로 치환한다")
	@Size(max = 64)
	String clientMessageId
) {
}
