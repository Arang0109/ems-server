package com.ensolution.ems.client_management.application.command.create;

public record CreatePreventionCommand(
	Long tenantId,
	Long stackId,
	String name,
	String unit,
	Double capacity,
	String targetName,
	String removalEfficiency
) {
}
