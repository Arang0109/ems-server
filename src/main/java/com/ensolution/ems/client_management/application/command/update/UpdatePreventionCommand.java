package com.ensolution.ems.client_management.application.command.update;

public record UpdatePreventionCommand(
	String name,
	Double capacity,
	String unit,
	String targetName,
	String removalEfficiency
) {
}
