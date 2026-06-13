package com.ensolution.ems.client_management.presentation.facility;

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
