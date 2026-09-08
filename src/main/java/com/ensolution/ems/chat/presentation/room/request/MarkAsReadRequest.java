package com.ensolution.ems.chat.presentation.room.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record MarkAsReadRequest(
	@Schema(description = "여기까지 읽었다는 메시지 id", example = "665f0000000000000000000a")
	@NotBlank(message = "읽은 위치를 지정해 주세요.")
	String lastReadMessageId
) {
}
