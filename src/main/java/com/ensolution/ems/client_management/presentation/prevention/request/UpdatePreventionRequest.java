package com.ensolution.ems.client_management.presentation.prevention.request;

import jakarta.validation.constraints.NotBlank;

public record UpdatePreventionRequest(
	@NotBlank(message = "방지시설명은 필수 입력값입니다.")
	String name,
	Double capacity,
	String unit,
	String targetName,
	String removalEfficiency
) {
}
