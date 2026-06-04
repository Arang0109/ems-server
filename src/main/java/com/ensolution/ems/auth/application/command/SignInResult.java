package com.ensolution.ems.auth.application.command;

public record SignInResult(
		String accessToken,
		String refreshToken,
		String username,
		String name,
		long tokenValidity
) {
}
