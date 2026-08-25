package com.ensolution.ems.auth.application.command;

public record SignInResult(
		String accessToken,
		String refreshToken,
		Long tenantId,
		String tenant,
		String username,
		String name,
		Long teamId,
		String teamName,
		String role,
		long tokenValidity
) {
}
