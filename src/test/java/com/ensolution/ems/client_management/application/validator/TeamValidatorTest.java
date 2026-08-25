package com.ensolution.ems.client_management.application.validator;

import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
import com.ensolution.ems.auth.application.port.in.UserSummary;
import com.ensolution.ems.client_management.application.FakeTeamRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 로그인 응답의 소속 팀이 유일하게 결정되도록 보장하는 1인 1팀 규칙 검증. */
class TeamValidatorTest {

	private static final Long TENANT = 1L;
	private static final Long OTHER_TENANT = 2L;

	/** 1인 1팀 규칙은 auth 조회를 하지 않는다. 호출되면 테스트가 실패하도록 미구현으로 둔다. */
	private static final UserQueryUseCase UNUSED_USER_QUERY = new UserQueryUseCase() {
		@Override
		public UserSummary getUser(Long userId, Long tenantId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<UserSummary> getUserList(Long tenantId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean existsByUsername(String username) {
			throw new UnsupportedOperationException();
		}
	};

	private FakeTeamRepository teamRepository;
	private TeamValidator validator;

	@BeforeEach
	void setUp() {
		teamRepository = new FakeTeamRepository();
		validator = new TeamValidator(teamRepository, UNUSED_USER_QUERY);
	}

	@Nested
	@DisplayName("requireNotAssignedToOtherTeam")
	class RequireNotAssignedToOtherTeam {

		@Test
		void 이미_다른_팀의_사수인_사용자는_거부한다() {
			teamRepository.given(1L, TENANT, "1팀", 10L, 20L);

			assertThatThrownBy(() -> validator.requireNotAssignedToOtherTeam(10L, TENANT, null))
				.isInstanceOf(CustomException.class)
				.extracting(e -> ((CustomException) e).getErrorCode())
				.isEqualTo(ErrorCode.TEAM_MEMBER_ALREADY_ASSIGNED);
		}

		@Test
		void 이미_다른_팀의_부사수인_사용자도_거부한다() {
			teamRepository.given(1L, TENANT, "1팀", 10L, 20L);

			assertThatThrownBy(() -> validator.requireNotAssignedToOtherTeam(20L, TENANT, null))
				.isInstanceOf(CustomException.class)
				.extracting(e -> ((CustomException) e).getErrorCode())
				.isEqualTo(ErrorCode.TEAM_MEMBER_ALREADY_ASSIGNED);
		}

		@Test
		void 배정_이력이_없으면_통과한다() {
			teamRepository.given(1L, TENANT, "1팀", 10L, 20L);

			assertThatCode(() -> validator.requireNotAssignedToOtherTeam(30L, TENANT, null))
				.doesNotThrowAnyException();
		}

		@Test
		void 수정_대상_팀에_배정된_경우는_자기_자신이므로_통과한다() {
			teamRepository.given(1L, TENANT, "1팀", 10L, 20L);

			assertThatCode(() -> validator.requireNotAssignedToOtherTeam(10L, TENANT, 1L))
				.doesNotThrowAnyException();
		}

		@Test
		void 수정_중이라도_다른_팀에_배정돼_있으면_거부한다() {
			teamRepository.given(1L, TENANT, "1팀", 10L, 20L);
			teamRepository.given(2L, TENANT, "2팀", 30L, 40L);

			assertThatThrownBy(() -> validator.requireNotAssignedToOtherTeam(30L, TENANT, 1L))
				.isInstanceOf(CustomException.class)
				.extracting(e -> ((CustomException) e).getErrorCode())
				.isEqualTo(ErrorCode.TEAM_MEMBER_ALREADY_ASSIGNED);
		}

		@Test
		void 다른_tenant의_배정은_영향을_주지_않는다() {
			teamRepository.given(1L, OTHER_TENANT, "타사 1팀", 10L, 20L);

			assertThatCode(() -> validator.requireNotAssignedToOtherTeam(10L, TENANT, null))
				.doesNotThrowAnyException();
		}

		@Test
		void 사용자_id가_없으면_통과한다() {
			teamRepository.given(1L, TENANT, "1팀", null, null);

			assertThatCode(() -> validator.requireNotAssignedToOtherTeam(null, TENANT, null))
				.doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("requireDistinctMembers")
	class RequireDistinctMembers {

		@Test
		void 사수와_부사수가_같으면_거부한다() {
			assertThatThrownBy(() -> validator.requireDistinctMembers(10L, 10L))
				.isInstanceOf(CustomException.class)
				.extracting(e -> ((CustomException) e).getErrorCode())
				.isEqualTo(ErrorCode.TEAM_MEMBER_DUPLICATED);
		}

		@Test
		void 사수와_부사수가_다르면_통과한다() {
			assertThatCode(() -> validator.requireDistinctMembers(10L, 20L))
				.doesNotThrowAnyException();
		}

		@Test
		void 한쪽이_비어_있으면_통과한다() {
			assertThatCode(() -> validator.requireDistinctMembers(null, null))
				.doesNotThrowAnyException();
			assertThatCode(() -> validator.requireDistinctMembers(10L, null))
				.doesNotThrowAnyException();
		}
	}
}
