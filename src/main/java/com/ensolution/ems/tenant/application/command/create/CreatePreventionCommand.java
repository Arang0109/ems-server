package com.ensolution.ems.tenant.application.command.create;

public record CreatePreventionCommand(
	Long tenantId,
	Long stackId,
	String name
) {
}
