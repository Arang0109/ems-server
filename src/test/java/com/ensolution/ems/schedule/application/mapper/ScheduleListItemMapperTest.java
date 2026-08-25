package com.ensolution.ems.schedule.application.mapper;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.schedule.application.command.list_item.ScheduleListItem;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.domain.snapshot.ClientSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.StackSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.TeamSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.WorkplaceSnapshot;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 메타 필드는 그대로 옮기고, 표시용 이름 3개는 스냅샷 트리에서 뽑되 어느 단계가 비어도
 * 예외 없이 null로 내려가는지 검증한다.
 */
class ScheduleListItemMapperTest {

	private final ScheduleListItemMapper mapper = new ScheduleListItemMapper();

	private static final LocalDate SAMPLED_AT = LocalDate.of(2026, 8, 18);
	private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 1, 9, 30);

	private Schedule meta() {
		return Schedule.builder()
			.id(11L)
			.tenantId(1L)
			.stackId(22L)
			.teamId(33L)
			.measurementField(MeasurementField.AIR)
			.sampledAt(SAMPLED_AT)
			.schedulePurpose("정기 측정")
			.status(ScheduleStatus.MEASURING)
			.referenceNumber("2026-A-001")
			.createdAt(CREATED_AT)
			.build();
	}

	private ScheduleSnapshot snapshot(ClientSnapshot client, TeamSnapshot team) {
		return new ScheduleSnapshot("11", 11L, 1L, ScheduleStatus.MEASURING, null,
			team, null, client, null, null, null, null, null);
	}

	private ClientSnapshot client(WorkplaceSnapshot workplace) {
		return new ClientSnapshot(
			101L, "가나다환경", null, null, null, null, null, null, null, workplace);
	}

	private WorkplaceSnapshot workplace(StackSnapshot stack) {
		return new WorkplaceSnapshot(
			201L, "1사업장", null, null, null, null, null, null, stack);
	}

	private StackSnapshot stack() {
		return new StackSnapshot(
			301L, MeasurementField.AIR, "1호 배출구", null, null, null, null,
			null, null, null, null, null, null, null);
	}

	private TeamSnapshot team() {
		return new TeamSnapshot(401L, "1팀", null, null, null, null, null, null, null, null);
	}

	@Test
	void 메타와_완전한_스냅샷을_합쳐_모든_필드를_채운다() {
		ScheduleListItem item = mapper.toListItem(meta(), snapshot(client(workplace(stack())), team()));

		assertThat(item.id()).isEqualTo(11L);
		assertThat(item.stackId()).isEqualTo(22L);
		assertThat(item.teamId()).isEqualTo(33L);
		assertThat(item.measurementField()).isEqualTo(MeasurementField.AIR);
		assertThat(item.sampledAt()).isEqualTo(SAMPLED_AT);
		assertThat(item.schedulePurpose()).isEqualTo("정기 측정");
		assertThat(item.status()).isEqualTo(ScheduleStatus.MEASURING);
		assertThat(item.referenceNumber()).isEqualTo("2026-A-001");
		assertThat(item.createdAt()).isEqualTo(CREATED_AT);

		assertThat(item.clientName()).isEqualTo("가나다환경");
		assertThat(item.stackName()).isEqualTo("1호 배출구");
		assertThat(item.teamName()).isEqualTo("1팀");
	}

	@Test
	void 스냅샷이_없으면_메타만_채우고_표시용_이름은_비운다() {
		ScheduleListItem item = mapper.toListItem(meta(), null);

		assertThat(item.id()).isEqualTo(11L);
		assertThat(item.referenceNumber()).isEqualTo("2026-A-001");
		assertThat(item.clientName()).isNull();
		assertThat(item.stackName()).isNull();
		assertThat(item.teamName()).isNull();
	}

	@Test
	void 의뢰기관이_비면_의뢰기관명과_측정시설명만_비운다() {
		ScheduleListItem item = mapper.toListItem(meta(), snapshot(null, team()));

		assertThat(item.clientName()).isNull();
		assertThat(item.stackName()).isNull();
		assertThat(item.teamName()).isEqualTo("1팀");
	}

	@Test
	void 사업장이_비면_측정시설명만_비운다() {
		ScheduleListItem item = mapper.toListItem(meta(), snapshot(client(null), team()));

		assertThat(item.clientName()).isEqualTo("가나다환경");
		assertThat(item.stackName()).isNull();
		assertThat(item.teamName()).isEqualTo("1팀");
	}

	@Test
	void 측정시설이_비면_측정시설명만_비운다() {
		ScheduleListItem item = mapper.toListItem(meta(), snapshot(client(workplace(null)), team()));

		assertThat(item.clientName()).isEqualTo("가나다환경");
		assertThat(item.stackName()).isNull();
		assertThat(item.teamName()).isEqualTo("1팀");
	}

	@Test
	void 팀이_비면_팀명만_비운다() {
		ScheduleListItem item = mapper.toListItem(meta(), snapshot(client(workplace(stack())), null));

		assertThat(item.clientName()).isEqualTo("가나다환경");
		assertThat(item.stackName()).isEqualTo("1호 배출구");
		assertThat(item.teamName()).isNull();
	}
}
