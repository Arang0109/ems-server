package com.ensolution.ems.client_management.application.command.update;

public record UpdateTargetSubstanceCommand(
	String name,
	Double removalEfficiency
) {
}
