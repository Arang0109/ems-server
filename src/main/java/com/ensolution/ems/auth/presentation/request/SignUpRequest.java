package com.ensolution.ems.auth.presentation.request;

import jakarta.validation.constraints.NotBlank;

public record SignUpRequest (
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