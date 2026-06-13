package com.ensolution.ems.client_management.application.command.create;

public record CreateTargetSubstanceCommand(
	Long preventionId,
	String name,
	Double removalEfficiency
) {
}
