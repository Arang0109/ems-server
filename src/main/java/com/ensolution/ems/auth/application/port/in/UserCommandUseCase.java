package com.ensolution.ems.auth.application.port.in;

public interface UserCommandUseCase {
	/** 테넌트 범위 회원 생성. PLATFORM_ADMIN 역할은 부여할 수 없다(ROLE_NOT_ASSIGNABLE). */
	void createUser(CreateUserCommand command);

	/**
	 * 부트스트랩 전용 운영자 계정 생성. PLATFORM_ADMIN 부여 제한을 적용하지 않는 <b>유일한</b> 경로다.
	 * {@code PlatformAdminInitializer}(배포 1회성 설치 코드) 외에서 호출하지 않는다.
	 */
	void createPlatformAdmin(CreateUserCommand command);
	void updateUser(UpdateUserCommand command);
	/** tenant 범위 삭제. 미존재·타 tenant 모두 USER_NOT_FOUND로 은닉한다. */
	void deleteUser(Long userId, Long tenantId);
}
