package com.ensolution.ems.client_management.presentation.facility.request;

import jakarta.validation.constraints.NotNull;

public record CreateFacilityRequest(
	@NotNull
	Long stackId,
	String name,
	String fuelUsage,
	String productOutput,
	String incinerationAmount,
	String fuelInput,
	String fuelType,
	String unit
) {
}
