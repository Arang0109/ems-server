package com.ensolution.ems.tenant.presentation.facility.request;

import jakarta.validation.constraints.NotNull;

public record CreateFacilityRequest(
	@NotNull
	Long stackId,
	String name,
	String fuelUsage,
	String fuelInput,
	String fuelType
) {
}
