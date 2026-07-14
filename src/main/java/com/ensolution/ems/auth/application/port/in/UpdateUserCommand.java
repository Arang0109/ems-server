package com.ensolution.ems.auth.application.port.in;

public record UpdateUserCommand(
	Long userId,
	Long roleId,
	String name,
	String department,
	String email,
	String tel
) {}
