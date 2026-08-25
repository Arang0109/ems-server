package com.ensolution.ems.schedule.presentation.mapper;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.domain.sheet.MeasurementCategory;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import com.ensolution.ems.schedule.domain.snapshot.BasicInfo;
import com.ensolution.ems.schedule.domain.snapshot.ClientSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.FacilitySnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.StackSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.TeamSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.TenantSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.WorkplaceSnapshot;
import com.ensolution.ems.schedule.presentation.response.snapshot.ScheduleSnapshotResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스냅샷 응답 DTO가 트리를 온전히 옮기면서 문서의 저장 메타는 내보내지 않는지 검증한다.
 * 메타(MySQL)가 진실의 원천이므로 식별자·상태 사본은 응답에 실리지 않아야 하고,
 * 반대로 시트 버전은 클라이언트가 되돌려 보내야 하는 값이라 반드시 남아야 한다.
 */
class ScheduleMapperSnapshotTest {

	private final ScheduleMapper mapper = new ScheduleMapperImpl();

	private static final LocalDate SAMPLED_AT = LocalDate.of(2026, 8, 18);

	private ScheduleSnapshot snapshot() {
		BasicInfo basicInfo = new BasicInfo(
			"2026-A-001", "관리자", "입회자", "분석자", "책임자",
			SAMPLED_AT, LocalDate.of(2026, 8, 19), null, null,
			null, null,
			MeasurementField.AIR, "자가측정용");

		StackSnapshot stack = new StackSnapshot(
			22L, MeasurementField.AIR, "1호 굴뚝", "SEMS-1", null, "제품",
			6, 30.0, 1.2, 1.2, null, null,
			List.of(new FacilitySnapshot(41L, "보일러", "100", "200", null, "300", "LNG", "N㎥")),
			List.of());

		WorkplaceSnapshot workplace = new WorkplaceSnapshot(
			21L, "사업장", "111-11-11111", "제조업", "도로명", "상세", "12345", null, stack);

		ClientSnapshot client = new ClientSnapshot(
			20L, "의뢰기관", "222-22-22222", "대표", "도로명", "상세", "54321",
			"a@b.com", "02-000-0000", workplace);

		MeasurementSheet sheet = MeasurementSheet.builder()
			.category(MeasurementCategory.GAS)
			.version(7L)
			.build();

		return new ScheduleSnapshot(
			"11", 11L, 1L, ScheduleStatus.MEASURING, basicInfo,
			new TeamSnapshot(33L, "1팀", 1L, "멘토", 2L, "멘티", null, null, null, null),
			new TenantSnapshot(1L, "고객사", "333-33-33333", "대표", "도로명", "상세", "11111"),
			client, List.of(), List.of(), List.of(sheet),
			5L, LocalDateTime.of(2026, 8, 1, 9, 30));
	}

	@Test
	void 스냅샷_트리를_끝까지_옮긴다() {
		ScheduleSnapshotResponse response = mapper.toSnapshotResponse(snapshot());

		assertThat(response.basicInfo().referenceNumber()).isEqualTo("2026-A-001");
		assertThat(response.basicInfo().receivedAt()).isEqualTo(LocalDate.of(2026, 8, 19));
		assertThat(response.basicInfo().measurementField()).isEqualTo(MeasurementField.AIR);
		assertThat(response.team().teamName()).isEqualTo("1팀");
		assertThat(response.tenant().name()).isEqualTo("고객사");
		assertThat(response.client().workplace().stack().stackId()).isEqualTo(22L);
		assertThat(response.client().workplace().stack().standardOxygen()).isEqualTo(6);
		assertThat(response.client().workplace().stack().facilities())
			.singleElement()
			.satisfies(facility -> assertThat(facility.name()).isEqualTo("보일러"));
	}

	@Test
	void 시트_버전은_그대로_내려간다() {
		ScheduleSnapshotResponse response = mapper.toSnapshotResponse(snapshot());

		assertThat(response.sheets())
			.singleElement()
			.satisfies(sheet -> {
				assertThat(sheet.getCategory()).isEqualTo(MeasurementCategory.GAS);
				assertThat(sheet.getVersion()).isEqualTo(7L);
			});
	}

	@Test
	void 문서의_저장_메타는_응답에_실리지_않는다() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

		JsonNode root = objectMapper.readTree(
			objectMapper.writeValueAsString(mapper.toSnapshotResponse(snapshot())));

		assertThat(root.fieldNames()).toIterable()
			.containsExactlyInAnyOrder("basicInfo", "team", "tenant", "client", "equipments", "items", "sheets");
		assertThat(root.get("sheets").get(0).get("version").asLong()).isEqualTo(7L);
	}

	@Test
	void null_스냅샷은_null로_돌려준다() {
		assertThat(mapper.toSnapshotResponse(null)).isNull();
	}
}
