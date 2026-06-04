package com.ensolution.ems.auth.domain;

public record AuthenticatedUser (
	Long userId,
	String username,
	String name
) {}
