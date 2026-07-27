package com.ensolution.ems.tenant.application.command.update;

public record UpdateFacilityCommand(
	String name,
	String fuelUsage,
	String productOutput,
	String incinerationAmount,
	String fuelInput,
	String fuelType,
	String unit
) {
}
