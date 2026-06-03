package com.ensolution.ems.client_management.application.command;

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
