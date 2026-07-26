package com.ensolution.ems.auth.application.port.in;

/**
 * 타 모듈이 역할 이름으로 roleId를 해석할 때 사용하는 인바운드 포트.
 * (예: platform 모듈의 테넌트 발급 시 ADMIN roleId 조회, 운영자 부트스트랩 시 PLATFORM_ADMIN roleId 조회)
 */
public interface RoleQueryUseCase {
	Long findRoleIdByName(String name);
}
