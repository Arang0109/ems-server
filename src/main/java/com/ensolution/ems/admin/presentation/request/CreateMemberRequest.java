package com.ensolution.ems.admin.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMemberRequest(
	@NotNull
	Long roleId,

	@NotBlank(message = "아이디는 필수 입력값입니다.")
	String username,
	@NotBlank(message = "패스워드는 필수 입력값입니다.")
	String password,
	String name,
	String department,
	String email,
	String tel
) {
}
