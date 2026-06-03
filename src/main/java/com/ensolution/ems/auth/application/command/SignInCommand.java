package com.ensolution.ems.auth.application.command;

public record SignInCommand(
		String username,
		String password
) {}