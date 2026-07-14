package com.ensolution.ems.admin.application.command;

public record UpdateMemberCommand(
	Long id,
	Long roleId,
	String name,
	String department,
	String email,
	String tel
) {
}
