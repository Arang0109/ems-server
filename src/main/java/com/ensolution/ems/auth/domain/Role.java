package com.ensolution.ems.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder(toBuilder = true)
public class Role {

	/**
	 * 플랫폼 운영자 역할명. 특정 테넌트에 속하지 않는 전역 역할이므로
	 * 테넌트 범위 API(회원 등록·수정)로는 부여할 수 없다.
	 */
	public static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";

	private Long roleId;
	private String name;
	private String description;

	public boolean isPlatformAdmin() {
		return PLATFORM_ADMIN.equals(name);
	}
}
