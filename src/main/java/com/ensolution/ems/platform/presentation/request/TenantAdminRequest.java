package com.ensolution.ems.platform.presentation.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 고객사 발급 시 함께 생성되는 초기 관리자(ADMIN) 계정 요청.
 */
public record TenantAdminRequest(
	@NotBlank(message = "관리자 아이디는 필수 입력값입니다.")
	String username,
	@NotBlank(message = "관리자 비밀번호는 필수 입력값입니다.")
	String password,
	@NotBlank(message = "관리자 이름은 필수 입력값입니다.")
	String name,
	String department,
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	String email,
	String tel
) {}
