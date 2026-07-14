package com.ensolution.ems.auth.domain;

public record AuthenticatedUser (
	Long userId,
	Long tenantId,
	String tenant,
	String username,
	String name,
	String role
) {}
