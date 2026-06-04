package com.ensolution.ems.client_management.application.command;

public record CreateWorkplaceCommand(
	Long companyId,
	String name,
	String address,
	String bizNumber
) {
}
