package com.ensolution.ems.client_management.presentation.facility.request;

public record UpdateFacilityRequest(
	String name,
	String fuelUsage,
	String productOutput,
	String incinerationAmount,
	String fuelInput,
	String fuelType,
	String unit
) {
}
