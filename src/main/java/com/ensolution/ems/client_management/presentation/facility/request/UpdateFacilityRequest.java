package com.ensolution.ems.client_management.presentation.facility.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateFacilityRequest(
	@NotBlank(message = "배출시설명 필수 입력값입니다.")
	String name,
	String fuelUsage,
	String productOutput,
	String incinerationAmount,
	String fuelInput,
	String fuelType,
	String unit
) {
}
