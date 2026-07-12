package com.ensolution.ems.client_management.presentation.prevention.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePreventionRequest(
	@NotNull
	Long stackId,
	@NotBlank(message = "방지설비명은 필수 입력값입니다.")
	String name
) {
}
