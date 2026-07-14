package com.ensolution.ems.tenant.presentation.facility.request;

public record UpdateFacilityRequest(
	String name,
	String fuelUsage,
	String fuelInput,
	String fuelType
) {
}
