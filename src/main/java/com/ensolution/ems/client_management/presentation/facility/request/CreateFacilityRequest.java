package com.ensolution.ems.client_management.presentation.facility.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateFacilityRequest(
	@NotNull
	Long stackId,
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
