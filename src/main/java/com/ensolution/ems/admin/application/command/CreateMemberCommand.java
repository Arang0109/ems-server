package com.ensolution.ems.admin.application.command;

public record CreateMemberCommand(
	Long tenantId,
	Long roleId,
	String username,
	String password,
	String name,
	String department,
	String email,
	String tel
) {
}
