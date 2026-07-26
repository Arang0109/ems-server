package com.ensolution.ems.auth.application.port.in;

import java.util.List;

public interface UserQueryUseCase {
	UserSummary getUser(Long userId);
	List<UserSummary> getUserList(Long tenantId);
	boolean existsByUsername(String username);
}
