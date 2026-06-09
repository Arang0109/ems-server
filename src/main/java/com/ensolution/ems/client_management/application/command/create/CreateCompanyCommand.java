package com.ensolution.ems.client_management.application.command.create;

public record CreateCompanyCommand(
	String name,
	String bizNumber,
	String representative,
	String address,
	
	String manager,
	String email,
	String tel
) {
}
