package com.ensolution.ems.client_management.presentation.request;

import jakarta.validation.constraints.NotBlank;

public record CreatePollutantRequest(
	@NotBlank(message = "측정물질 한국어명은 필수 입력값입니다.")
	String nameKr,
	String nameEn
) {}
