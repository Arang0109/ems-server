package com.ensolution.ems.client_management.presentation.facility;

public record UpdateFacilityRequest(
	String name,
	String fuelUsage,
	String fuelInput,
	String fuelType
) {
}
