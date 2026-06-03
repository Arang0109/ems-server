package com.ensolution.ems.auth.application.command;

public record SignUpCommand (
	String username,
	String password,
	String name,
	String department,
	String email,
	String tel
) {}
