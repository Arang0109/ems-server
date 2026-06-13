package com.ensolution.ems.client_management.application.command.update;

public record UpdateCompanyCommand(
	String name,
	String bizNumber,
	String representative,
	String zipcode,
	String roadAddress,
	String address,
	
	String manager,
	String email,
	String tel
) {
}
