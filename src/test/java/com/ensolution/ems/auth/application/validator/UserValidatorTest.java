package com.ensolution.ems.auth.application.validator;

import com.ensolution.ems.auth.application.FakeRoleRepository;
import com.ensolution.ems.auth.domain.Role;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 역할 부여 제한을 고정한다.
 * <p>
 * 이 검증은 <b>테넌트 격리로는 막을 수 없는 권한 상승</b>을 막는다 — 테넌트 ADMIN이 자기 계정의 역할을
 * {@code PLATFORM_ADMIN}으로 바꾸는 경로는 tenant 범위를 한 번도 벗어나지 않기 때문에
 * {@code findById(id, tenantId)}로는 걸리지 않는다. 루트 {@code CLAUDE.md} 규칙 13이 역할 부여를
 * 별도 규칙으로 떼어 둔 이유이며, 그 규칙을 코드로 고정하는 것이 이 테스트다.
 * <p>
 * 함께 고정하는 것: <b>"역할이 존재하는가"와 "부여할 수 있는가"는 다른 질문</b>이라는 점.
 * 두 응답({@code ROLE_NOT_FOUND} / {@code ROLE_NOT_ASSIGNABLE})이 섞이면
 * 존재 확인을 권한 검증으로 착각하는 실수가 되살아난다.
 */
class UserValidatorTest {

	private static final long ADMIN_ROLE_ID = 1L;
	private static final long USER_ROLE_ID = 2L;
	private static final long PLATFORM_ADMIN_ROLE_ID = 99L;

	private final FakeRoleRepository roleRepository = new FakeRoleRepository();
	private final UserValidator validator = new UserValidator(roleRepository);

	UserValidatorTest() {
		roleRepository.given(ADMIN_ROLE_ID, "ADMIN");
		roleRepository.given(USER_ROLE_ID, "USER");
		roleRepository.given(PLATFORM_ADMIN_ROLE_ID, Role.PLATFORM_ADMIN);
	}

	@Nested
	@DisplayName("부여 차단")
	class Blocked {

		@Test
		@DisplayName("PLATFORM_ADMIN 역할은 부여할 수 없다")
		void 운영자_역할은_부여할_수_없다() {
			assertThatThrownBy(() -> validator.requireAssignableRole(PLATFORM_ADMIN_ROLE_ID))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLE_NOT_ASSIGNABLE);
		}

		@Test
		@DisplayName("ROLE_NOT_ASSIGNABLE은 403이다 — 존재를 숨기는 404가 아니라 거부를 알린다")
		void 부여_거부는_403이다() {
			assertThatThrownBy(() -> validator.requireAssignableRole(PLATFORM_ADMIN_ROLE_ID))
				.isInstanceOf(CustomException.class)
				.extracting(e -> ((CustomException) e).getErrorCode().getStatus().value())
				.isEqualTo(403);
		}
	}

	@Nested
	@DisplayName("통과")
	class Allowed {

		@Test
		@DisplayName("테넌트 역할은 부여할 수 있다")
		void 테넌트_역할은_통과한다() {
			assertThatCode(() -> validator.requireAssignableRole(ADMIN_ROLE_ID)).doesNotThrowAnyException();
			assertThatCode(() -> validator.requireAssignableRole(USER_ROLE_ID)).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("roleId가 null이면 역할 변경이 없는 것으로 보고 통과시킨다")
		void null이면_역할_변경이_아니다() {
			assertThatCode(() -> validator.requireAssignableRole(null)).doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("존재 확인과 권한 검증의 구분")
	class NotFoundVsNotAssignable {

		@Test
		@DisplayName("존재하지 않는 역할은 ROLE_NOT_FOUND — 부여 거부와 섞이지 않는다")
		void 없는_역할은_NOT_FOUND다() {
			assertThatThrownBy(() -> validator.requireAssignableRole(404L))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLE_NOT_FOUND);
		}
	}
}
