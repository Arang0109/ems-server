package com.ensolution.ems.tenant.application.command.create;

public record CreateFacilityCommand(
	Long tenantId,
	Long stackId,
	String name,
	String fuelUsage,
	String fuelInput,
	String fuelType
) {
}
