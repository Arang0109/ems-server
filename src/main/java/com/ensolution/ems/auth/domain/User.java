package com.ensolution.ems.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder(toBuilder = true)
public class User {
	private Long id;
	private Long tenantId;
	private Long roleId;
	private String username;
	private String password;
	private String name;
	private String department;
	private String email;
	private String tel;

	public static User signUp(
		Long tenantId,
		Long roleId,
		String username,
		String encodedPassword,
		String name,
		String department,
		String email,
		String tel
	) {
		return User.builder()
			.tenantId(tenantId)
			.roleId(roleId)
			.username(username)
			.password(encodedPassword)
			.name(name)
			.department(department)
			.email(email)
			.tel(tel)
			.build();
	}

	public User update(
		Long roleId,
		String name,
		String department,
		String email,
		String tel
	) {
		return this.toBuilder()
			.roleId(roleId == null ? this.roleId : roleId)
			.name(keep(name, this.name))
			.department(keep(department, this.department))
			.email(keep(email, this.email))
			.tel(keep(tel, this.tel))
			.build();
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}
}
