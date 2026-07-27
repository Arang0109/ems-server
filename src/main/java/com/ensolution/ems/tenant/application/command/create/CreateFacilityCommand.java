package com.ensolution.ems.tenant.application.command.create;

public record CreateFacilityCommand(
	Long tenantId,
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
