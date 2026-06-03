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
	private String username;
	private String password;
	private String name;
	private String department;
	private String email;
	private String tel;
	
	public static User signUp(
		String username,
		String encodedPassword,
		String name,
		String department,
		String email,
		String tel
	) {
		return User.builder()
			.username(username)
			.password(encodedPassword)
			.name(name)
			.department(department)
			.email(email)
			.tel(tel)
			.build();
	}
}
