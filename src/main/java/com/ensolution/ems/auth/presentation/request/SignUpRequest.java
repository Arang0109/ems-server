package com.ensolution.ems.auth.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SignUpRequest (
	@NotNull(message = "소속 고객사는 필수입니다.")
	Long tenantId,
	@NotNull(message = "역할은 필수입니다.")
	Long roleId,
	@NotBlank(message = "아이디는 필수 입력값입니다.")
	String username,
	@NotBlank(message = "비밀번호는 필수 입력값입니다.")
	String password,
	@NotBlank(message = "이름은 필수 입력값입니다.")
	String name,
	String department,
	String email,
	String tel
) {}