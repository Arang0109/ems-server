package com.ensolution.ems.auth.application.port.in;

import java.util.List;

public interface UserQueryUseCase {
	/** tenant 범위 단건 조회. 미존재·타 tenant 모두 USER_NOT_FOUND로 은닉한다. */
	UserSummary getUser(Long userId, Long tenantId);
	List<UserSummary> getUserList(Long tenantId);
	boolean existsByUsername(String username);
}
