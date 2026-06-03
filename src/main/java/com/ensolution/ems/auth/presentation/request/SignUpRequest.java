package com.ensolution.ems.auth.presentation.request;

public record SignUpRequest (
	String username,
	String password,
	String name,
	String department,
	String email,
	String tel
) {}
