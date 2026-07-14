package com.ensolution.ems.admin.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder(toBuilder = true)
public class Member {
	private Long id;
	private Long tenantId;
	private String username;
	private String name;
	private Long roleId;
	private String role;
	private String department;
	private String email;
	private String tel;
}
