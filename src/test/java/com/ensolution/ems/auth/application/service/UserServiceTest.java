package com.ensolution.ems.auth.application.service;

import com.ensolution.ems.auth.application.FakeRoleRepository;
import com.ensolution.ems.auth.application.FakeUserRepository;
import com.ensolution.ems.auth.application.port.in.UserSummary;
import com.ensolution.ems.auth.domain.User;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * 사용자 조회 계약을 고정한다.
 * <p>
 * 이 조회는 소비자가 둘이다 — ADMIN 전용 회원 관리({@code /api/admin/members})와
 * 인증된 사용자 전원에게 열린 선택지 목록({@code /api/users}). 뒤쪽이 있는 한
 * <b>목록에 남의 테넌트 사용자가 한 명이라도 섞이면 그대로 교차 테넌트 노출</b>이므로,
 * 여기서 고정하는 것은 "목록은 요청자 테넌트로 닫혀 있다"는 사실이다.
 * <p>
 * 역할 이름 결합도 함께 본다. 역할은 전역 마스터라 tenant 필터가 없어, 결합 방식이 바뀌어도
 * 격리는 깨지지 않는 대신 조용히 이름이 비는 형태로 회귀하기 쉽다.
 */
class UserServiceTest {

	private static final Long TENANT = 1L;
	private static final Long OTHER_TENANT = 2L;

	private static final Long ROLE_ADMIN = 10L;
	private static final Long ROLE_FIELD = 11L;

	private FakeUserRepository userRepository;
	private FakeRoleRepository roleRepository;
	private UserService userService;

	@BeforeEach
	void setUp() {
		userRepository = new FakeUserRepository();
		roleRepository = new FakeRoleRepository();
		roleRepository.given(ROLE_ADMIN, "ADMIN");
		roleRepository.given(ROLE_FIELD, "FIELD");

		userService = new UserService(userRepository, roleRepository);
	}

	@Nested
	@DisplayName("사용자 목록 조회")
	class GetUserList {

		@Test
		void 요청자_테넌트의_사용자만_반환한다() {
			givenUser(TENANT, ROLE_FIELD, "kim", "측정1팀");
			givenUser(OTHER_TENANT, ROLE_FIELD, "lee", "측정2팀");

			List<UserSummary> summaries = userService.getUserList(TENANT);

			assertThat(summaries)
				.extracting(UserSummary::username)
				.containsExactly("kim");
		}

		@Test
		void 역할_이름을_붙여_반환한다() {
			givenUser(TENANT, ROLE_ADMIN, "admin", "관리부");
			givenUser(TENANT, ROLE_FIELD, "kim", "측정1팀");

			List<UserSummary> summaries = userService.getUserList(TENANT);

			assertThat(summaries)
				.extracting(UserSummary::name, UserSummary::department, UserSummary::role)
				.containsExactly(
					tuple("admin", "관리부", "ADMIN"),
					tuple("kim", "측정1팀", "FIELD")
				);
		}

		@Test
		void 사용자가_없는_테넌트는_빈_목록이다() {
			givenUser(OTHER_TENANT, ROLE_FIELD, "lee", "측정2팀");

			assertThat(userService.getUserList(TENANT)).isEmpty();
		}
	}

	@Nested
	@DisplayName("사용자 단건 조회")
	class GetUser {

		@Test
		void 다른_테넌트의_사용자는_USER_NOT_FOUND다() {
			User other = givenUser(OTHER_TENANT, ROLE_FIELD, "lee", "측정2팀");

			assertThatThrownBy(() -> userService.getUser(other.getId(), TENANT))
				.isInstanceOf(CustomException.class)
				.extracting(e -> ((CustomException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
		}
	}

	private User givenUser(Long tenantId, Long roleId, String name, String department) {
		return userRepository.save(User.builder()
			.tenantId(tenantId)
			.roleId(roleId)
			.username(name)
			.password("encoded:" + name)
			.name(name)
			.department(department)
			.build());
	}
}
