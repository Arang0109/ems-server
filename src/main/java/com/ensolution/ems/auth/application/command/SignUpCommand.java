package com.ensolution.ems.auth.application.command;

public record SignUpCommand (
	Long tenantId,
	Long roleId,
	String username,
	String password,
	String name,
	String department,
	String email,
	String tel
) {}
