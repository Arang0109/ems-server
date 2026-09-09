package com.ensolution.ems.chat.presentation.room.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 대화 상대만 받는다. <b>내 id도 tenantId도 받지 않는다</b> — 둘 다 인증 주체에서 온다.
 * 클라이언트가 보낸 식별자를 신뢰하면 남의 이름으로 대화방을 열 수 있다.
 */
public record OpenDirectRoomRequest(
	@Schema(description = "대화 상대 사용자 id", example = "37")
	@NotNull(message = "대화 상대를 지정해 주세요.")
	Long counterpartId
) {
}
