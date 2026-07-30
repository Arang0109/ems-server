package com.ensolution.ems.client_management.presentation.facility.response;

public record FacilityResponse(
	Long id,
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
