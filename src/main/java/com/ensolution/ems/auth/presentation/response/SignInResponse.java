package com.ensolution.ems.auth.presentation.response;

public record SignInResponse (
	String accessToken,
	String tenant,
	String username,
	String name,
	String role
) {}
