package com.ensolution.ems.tenant.application.command.update;

public record UpdateTargetSubstanceCommand(
	String name,
	Double removalEfficiency
) {
}
