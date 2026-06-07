package com.ensolution.ems.client_management.application.command;

public record AssignStackPollutantCommand(
	Long stackId,
	Long pollutantId
) {}
