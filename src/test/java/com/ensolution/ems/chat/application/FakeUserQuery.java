package com.ensolution.ems.chat.application;

import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
import com.ensolution.ems.auth.application.port.in.UserSummary;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * tenant 범위 사용자 조회를 재현하는 {@link UserQueryUseCase}.
 *
 * <p>타 tenant 사용자는 <b>존재하지 않는 것으로</b> 답한다 — 포트 javadoc이 선언한 규약이며,
 * 채팅이 "상대가 같은 고객사인가"를 이 조회 하나에 맡기고 있으므로 여기서 어긋나면
 * 교차 테넌트 검증이 통째로 무의미해진다.
 */
public class FakeUserQuery implements UserQueryUseCase {

	private final List<UserSummary> users = new ArrayList<>();

	public void given(Long userId, Long tenantId, String name, String department) {
		users.add(new UserSummary(userId, tenantId, "user" + userId, name, 1L, "USER", department, null, null));
	}

	@Override
	public UserSummary getUser(Long userId, Long tenantId) {
		return users.stream()
			.filter(user -> Objects.equals(user.userId(), userId))
			.filter(user -> Objects.equals(user.tenantId(), tenantId))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}

	@Override
	public List<UserSummary> getUserList(Long tenantId) {
		return users.stream().filter(user -> Objects.equals(user.tenantId(), tenantId)).toList();
	}

	@Override
	public boolean existsByUsername(String username) {
		throw new UnsupportedOperationException();
	}
}
