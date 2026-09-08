package com.ensolution.ems.schedule.application.service.assembler;

import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
import com.ensolution.ems.auth.application.port.in.UserSummary;
import com.ensolution.ems.client_management.application.port.in.StackMeasurementItemSummary;
import com.ensolution.ems.client_management.application.port.in.StackMeasurementSummary;
import com.ensolution.ems.client_management.application.port.in.StackQueryUseCase;
import com.ensolution.ems.client_management.application.port.in.TeamQueryUseCase;
import com.ensolution.ems.client_management.application.port.in.TeamSummary;
import com.ensolution.ems.client_management.application.port.in.UserTeamSummary;
import com.ensolution.ems.equipment.application.port.in.EquipmentQueryUseCase;
import com.ensolution.ems.equipment.application.port.in.EquipmentSummary;
import com.ensolution.ems.equipment.application.port.in.InspectionDueSummary;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.platform.application.port.in.TenantQueryUseCase;
import com.ensolution.ems.platform.application.port.in.TenantSummary;
import com.ensolution.ems.schedule.application.mapper.ScheduleSnapshotPortMapperImpl;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 측정계획 등록 시 팀 스냅샷의 <b>측정자 표기</b>가 어디에서 오는지 고정한다.
 *
 * <p>사수·부사수는 팀 원장의 사수·부사수가 아니라 그 회차에 배정된 사람이다. 그래서 계획이 id를 갖고
 * 이름은 auth 원장에서 읽어 스냅샷에 찍는다. <b>배정하지 않으면 팀 원장의 이름이 그대로 남아야 한다</b> —
 * 이 폴백이 깨지면 사수·부사수를 보내지 않던 기존 클라이언트의 성적서 표기가 빈칸이 된다.
 */
class ScheduleSnapshotAssemblerTeamTest {

	private static final Long TENANT = 1L;
	private static final Long STACK = 22L;
	private static final Long TEAM = 33L;

	private StubUserQuery userQuery;
	private ScheduleSnapshotAssembler assembler;

	@BeforeEach
	void setUp() {
		userQuery = new StubUserQuery();
		assembler = new ScheduleSnapshotAssembler(
			STACK_QUERY, TEAM_QUERY, TENANT_QUERY, UNUSED_EQUIPMENT_QUERY,
			userQuery, new ScheduleSnapshotPortMapperImpl());
	}

	private Schedule meta(Long mentorId, Long menteeId) {
		return Schedule.register(
			TENANT, STACK, TEAM, mentorId, menteeId,
			MeasurementField.AIR, "정기 측정", "2026-A-001", LocalDate.of(2026, 9, 8)
		).toBuilder().id(11L).build();
	}

	@Nested
	@DisplayName("측정자 표기")
	class Measurers {

		@Test
		void 배정된_측정자의_이름으로_팀_기본값을_덮어쓴다() {
			userQuery.given(10L, TENANT, "김사수");
			userQuery.given(20L, TENANT, "이부사수");

			ScheduleSnapshot snapshot = assembler.assemble(meta(10L, 20L), List.of());

			assertThat(snapshot.team().mentorName()).isEqualTo("김사수");
			assertThat(snapshot.team().menteeName()).isEqualTo("이부사수");
		}

		@Test
		void 미배정이면_팀_원장의_이름이_남는다() {
			ScheduleSnapshot snapshot = assembler.assemble(meta(null, null), List.of());

			assertThat(snapshot.team().mentorName()).isEqualTo("팀사수");
			assertThat(snapshot.team().menteeName()).isEqualTo("팀부사수");
		}

		@Test
		void 한쪽만_배정하면_나머지는_팀_원장의_이름을_유지한다() {
			userQuery.given(10L, TENANT, "김사수");

			ScheduleSnapshot snapshot = assembler.assemble(meta(10L, null), List.of());

			assertThat(snapshot.team().mentorName()).isEqualTo("김사수");
			assertThat(snapshot.team().menteeName()).isEqualTo("팀부사수");
		}

		@Test
		void 배정된_사용자가_없으면_조립을_멈춘다() {
			// 예외를 삼키면 팀 기본값이 그 사람 이름인 것처럼 성적서에 찍힌다.
			assertThatThrownBy(() -> assembler.assemble(meta(99L, null), List.of()))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
		}

		@Test
		void 팀_연결키와_팀명은_바뀌지_않는다() {
			userQuery.given(10L, TENANT, "김사수");

			ScheduleSnapshot snapshot = assembler.assemble(meta(10L, null), List.of());

			assertThat(snapshot.team().teamId()).isEqualTo(TEAM);
			assertThat(snapshot.team().teamName()).isEqualTo("1측정팀");
		}
	}

	// --- 스텁 ---------------------------------------------------------------

	/** tenant 필터링을 실제 어댑터 그대로 재현한다. 미존재·타 tenant 모두 USER_NOT_FOUND다. */
	private static class StubUserQuery implements UserQueryUseCase {

		private final Map<Long, UserSummary> users = new HashMap<>();

		void given(Long userId, Long tenantId, String name) {
			users.put(userId, new UserSummary(userId, tenantId, "user" + userId, name, 10L, "FIELD", null, null, null));
		}

		@Override
		public UserSummary getUser(Long userId, Long tenantId) {
			UserSummary found = users.get(userId);
			if (found == null || !found.tenantId().equals(tenantId)) {
				throw new CustomException(ErrorCode.USER_NOT_FOUND);
			}
			return found;
		}

		@Override
		public List<UserSummary> getUserList(Long tenantId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean existsByUsername(String username) {
			throw new UnsupportedOperationException();
		}
	}

	/** 팀 원장의 사수·부사수 이름이 폴백의 출처다. 장비는 이 테스트의 관심사가 아니라 전부 비운다. */
	private static final TeamQueryUseCase TEAM_QUERY = new TeamQueryUseCase() {
		@Override
		public TeamSummary getTeamSummary(Long teamId, Long tenantId) {
			return new TeamSummary(teamId, "1측정팀", "팀사수", "팀부사수", null, null, null, null);
		}

		@Override
		public UserTeamSummary getUserTeamSummary(Long userId, Long tenantId) {
			throw new UnsupportedOperationException();
		}
	};

	private static final StackQueryUseCase STACK_QUERY = new StackQueryUseCase() {
		@Override
		public StackMeasurementSummary getMeasurementTargetSummary(Long stackId, Long tenantId) {
			return new StackMeasurementSummary(null, null, null, List.of(), List.of(), List.of());
		}

		@Override
		public long countStacks(Long tenantId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<StackMeasurementItemSummary> findMeasurementItems(Long tenantId, Long workplaceId, Long stackId) {
			throw new UnsupportedOperationException();
		}
	};

	private static final TenantQueryUseCase TENANT_QUERY =
		tenantId -> new TenantSummary(tenantId, "엔솔루션", null, null, null, null, null, null, null);

	/** 팀에 장비가 하나도 없으므로 이 경로는 장비 원장까지 가지 않는다. */
	private static final EquipmentQueryUseCase UNUSED_EQUIPMENT_QUERY = new EquipmentQueryUseCase() {
		@Override
		public EquipmentSummary getEquipmentSummary(String equipmentId, Long tenantId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<InspectionDueSummary> findInspectionDueBefore(Long tenantId, LocalDate dueDate) {
			throw new UnsupportedOperationException();
		}
	};
}
