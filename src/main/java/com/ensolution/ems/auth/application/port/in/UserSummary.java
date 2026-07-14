package com.ensolution.ems.auth.application.port.in;

import java.time.LocalDate;

public record UserSummary(
	Long userId,
	Long tenantId,
	String username,
	String name,
	
	Long roleId,
	String role,
	
	String department,
	String email,
	String tel
) {
}
