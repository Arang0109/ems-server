package com.ensolution.ems.auth.presentation.response;

public record RoleResponse(
	Long roleId,
	String name,
	String description
) {}
