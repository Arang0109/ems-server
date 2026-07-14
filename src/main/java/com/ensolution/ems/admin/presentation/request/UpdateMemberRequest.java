package com.ensolution.ems.admin.presentation.request;

import jakarta.validation.constraints.NotNull;

public record UpdateMemberRequest(
	@NotNull
	Long roleId,
	String name,
	String department,
	String email,
	String tel
) {
}
