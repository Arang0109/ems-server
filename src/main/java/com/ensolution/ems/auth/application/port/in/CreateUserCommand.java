package com.ensolution.ems.auth.application.port.in;

public record CreateUserCommand(
	Long tenantId,
	Long roleId,
	String username,
	String password,
	String name,
	String department,
	String email,
	String tel
) {}
