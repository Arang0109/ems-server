package com.ensolution.ems.auth.presentation.response;

public record SignInResponse (
	String accessToken,
	String tenant,
	String username,
	String name,
	Long teamId,
	String teamName,
	String role
) {}
