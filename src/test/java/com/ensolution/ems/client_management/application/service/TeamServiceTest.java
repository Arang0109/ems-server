package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
import com.ensolution.ems.auth.application.port.in.UserSummary;
import com.ensolution.ems.client_management.application.FakeTeamRepository;
import com.ensolution.ems.client_management.application.port.in.UserTeamSummary;
import com.ensolution.ems.client_management.application.validator.TeamValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 로그인 응답에 실리는 소속 팀 조회(getUserTeamSummary) 규칙 검증. */
class TeamServiceTest {

	private static final Long TENANT = 1L;
	private static final Long OTHER_TENANT = 2L;

	/** 소속 팀 조회는 사수·부사수 이름 조립을 하지 않는다. 호출되면 테스트가 실패하도록 미구현으로 둔다. */
	private static final UserQueryUseCase UNUSED_USER_QUERY = new UserQueryUseCase() {
		@Override
		public UserSummary getUser(Long userId) {
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
	private TeamService teamService;

	@BeforeEach
	void setUp() {
		teamRepository = new FakeTeamRepository();
		teamService = new TeamService(
			teamRepository,
			new TeamValidator(teamRepository, UNUSED_USER_QUERY),
			new TeamAssembler(UNUSED_USER_QUERY)
		);
	}

	@Nested
	@DisplayName("getUserTeamSummary")
	class GetUserTeamSummary {

		@Test
		void 사수로_배정된_팀의_id와_이름을_반환한다() {
			teamRepository.given(1L, TENANT, "1팀", 10L, 20L);

			UserTeamSummary summary = teamService.getUserTeamSummary(10L, TENANT);

			assertThat(summary).isEqualTo(new UserTeamSummary(1L, "1팀"));
		}

		@Test
		void 부사수로_배정된_팀도_반환한다() {
			teamRepository.given(1L, TENANT, "1팀", 10L, 20L);

			UserTeamSummary summary = teamService.getUserTeamSummary(20L, TENANT);

			assertThat(summary).isEqualTo(new UserTeamSummary(1L, "1팀"));
		}

		@Test
		void 소속_팀이_없으면_예외_없이_null을_반환한다() {
			teamRepository.given(1L, TENANT, "1팀", 10L, 20L);

			assertThat(teamService.getUserTeamSummary(30L, TENANT)).isNull();
		}

		@Test
		void 다른_tenant의_팀은_조회되지_않는다() {
			teamRepository.given(1L, OTHER_TENANT, "타사 1팀", 10L, 20L);

			assertThat(teamService.getUserTeamSummary(10L, TENANT)).isNull();
		}

		@Test
		void 기존_중복_배정_데이터가_있으면_teamId가_가장_작은_팀을_반환한다() {
			teamRepository.given(2L, TENANT, "2팀", 10L, 40L);
			teamRepository.given(1L, TENANT, "1팀", 10L, 20L);

			UserTeamSummary summary = teamService.getUserTeamSummary(10L, TENANT);

			assertThat(summary).isEqualTo(new UserTeamSummary(1L, "1팀"));
		}
	}
}
