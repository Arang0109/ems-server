package com.ensolution.ems.tenant.presentation.facility.response;

public record FacilityResponse(
	Long id,
	Long stackId,
	String name,
	String fuelUsage,
	String fuelInput,
	String fuelType
) {
}
