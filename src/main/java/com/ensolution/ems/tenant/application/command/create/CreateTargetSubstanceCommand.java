package com.ensolution.ems.tenant.application.command.create;

public record CreateTargetSubstanceCommand(
	Long tenantId,
	Long preventionId,
	String name,
	Double removalEfficiency
) {
}
