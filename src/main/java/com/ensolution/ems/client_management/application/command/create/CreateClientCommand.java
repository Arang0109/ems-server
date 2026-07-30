package com.ensolution.ems.client_management.application.command.create;

public record CreateClientCommand(
	Long tenantId,
	String name,
	String bizNumber,
	String representative,
	String roadAddress,
	String detailAddress,
	String zipcode,

	String manager,
	String email,
	String tel
) {
}
