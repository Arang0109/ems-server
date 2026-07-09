package com.ensolution.ems.client_management.presentation.facility.response;

public record FacilityResponse(
	Long id,
	Long stackId,
	String name,
	String fuelUsage,
	String fuelInput,
	String fuelType
) {
}
