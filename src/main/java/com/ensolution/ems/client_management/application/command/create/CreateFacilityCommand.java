package com.ensolution.ems.client_management.application.command.create;

public record CreateFacilityCommand(
	Long stackId,
	String name,
	String fuelUsage,
	String fuelInput,
	String fuelType
) {
}
