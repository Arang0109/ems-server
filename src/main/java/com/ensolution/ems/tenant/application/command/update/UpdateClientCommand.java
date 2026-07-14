package com.ensolution.ems.tenant.application.command.update;

public record UpdateClientCommand(
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
