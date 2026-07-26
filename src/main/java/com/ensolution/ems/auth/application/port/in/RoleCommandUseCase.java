package com.ensolution.ems.auth.application.port.in;

/**
 * 역할을 멱등하게 확보(존재하면 그대로, 없으면 생성)하는 인바운드 포트.
 * 서버 최초 배포 시 표준 역할·PLATFORM_ADMIN 역할을 시드하는 부트스트랩에서 사용한다.
 */
public interface RoleCommandUseCase {
	/**
	 * 이름으로 역할을 확보한다. 이미 존재하면 해당 roleId를, 없으면 생성 후 roleId를 반환한다.
	 */
	Long ensureRole(String name, String description);
}
