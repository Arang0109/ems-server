package com.ensolution.ems.platform.application.command.create;

/**
 * 고객사 발급 시 함께 생성되는 초기 관리자(ADMIN) 계정 정보.
 */
public record TenantAdminCommand(
	String username,
	String password,
	String name,
	String department,
	String email,
	String tel
) {}
