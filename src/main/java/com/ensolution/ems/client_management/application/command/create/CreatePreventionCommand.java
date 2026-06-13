package com.ensolution.ems.client_management.application.command.create;

public record CreatePreventionCommand(
	Long stackId,
	String name
) {
}
