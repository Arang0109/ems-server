package com.ensolution.ems.admin.presentation.response;

public record MemberResponse(
	Long id,
	String username,
	String name,
	Long roleId,
	String role,
	String department,
	String email,
	String tel,
	Long tenantId
) {
}
