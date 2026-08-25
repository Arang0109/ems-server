package com.ensolution.ems.admin.application.command;

public record UpdateMemberCommand(
	Long id,
	Long tenantId,
	Long roleId,
	String name,
	String department,
	String email,
	String tel
) {
}
