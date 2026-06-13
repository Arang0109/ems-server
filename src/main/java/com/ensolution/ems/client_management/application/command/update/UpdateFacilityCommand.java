package com.ensolution.ems.client_management.application.command.update;

public record UpdateFacilityCommand(
	String name,
	String fuelUsage,
	String fuelInput,
	String fuelType
) {
}
