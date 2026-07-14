package com.ensolution.ems.tenant.application.command.update;

public record UpdateFacilityCommand(
	String name,
	String fuelUsage,
	String fuelInput,
	String fuelType
) {
}
