package com.ensolution.ems.client_management.application.command;

public record UpdateWorkplaceCommand(
	String name,
	String address,
	String bizNumber
) {
}
