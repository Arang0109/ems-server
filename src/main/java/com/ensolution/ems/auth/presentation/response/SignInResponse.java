package com.ensolution.ems.auth.presentation.response;

public record SignInResponse (
	String accessToken,
	String username,
	String name
) {}