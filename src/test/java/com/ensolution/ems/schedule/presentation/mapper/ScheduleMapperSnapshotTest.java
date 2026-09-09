package com.ensolution.ems.schedule.presentation.mapper;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.domain.sampling.MeasurementCategory;
import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;
import com.ensolution.ems.schedule.domain.snapshot.AnalysisResult;
import com.ensolution.ems.schedule.domain.snapshot.ClientSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.FacilitySnapshot;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.SamplingSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.StackSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.TeamSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.TenantSnapshot;
import com.ensolution.ems.schedule.application.command.detail.ScheduleDetail;
import com.ensolution.ems.schedule.presentation.response.ScheduleResponse;
import com.ensolution.ems.schedule.presentation.response.snapshot.ScheduleSnapshotResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스냅샷 응답 DTO가 트리를 온전히 옮기면서 메타와 겹치는 값·문서의 저장 메타는 내보내지 않는지 검증한다.
 *
 * <p>메타(MySQL)가 진실의 원천이므로 관리번호·측정분야·일자와 상태는 응답 최상위에만 실려야 하고,
 * 반대로 시트 버전은 클라이언트가 되돌려 보내야 하는 값이라 반드시 남아야 한다.
 * 장비는 팀 아래, 기록지는 채취 정보 아래로 <b>한 번만</b> 나가는 것도 함께 고정한다.
 */
class ScheduleMapperSnapshotTest {

	private final ScheduleMapper mapper = new ScheduleMapperImpl();

	private ScheduleSnapshot snapshot() {
		StackSnapshot stack = new StackSnapshot(
			22L, MeasurementField.AIR, "1호 굴뚝", "SEMS-1", null, "제품",
			6, 30.0, 1.2, 1.2, null, null,
			List.of(new FacilitySnapshot(41L, "보일러", "100", "200", null, "300", "LNG", "N㎥")),
			List.of());

		ClientSnapshot client = new ClientSnapshot(
			20L, "의뢰기관", "222-22-22222", "대표", "도로명", "상세", "54321",
			"a@b.com", "02-000-0000",
			new com.ensolution.ems.schedule.domain.snapshot.WorkplaceSnapshot(
				21L, "사업장", "111-11-11111", "제조업", "도로명", "상세", "12345", null, stack));

		SamplingSheet sheet = SamplingSheet.builder()
			.category(MeasurementCategory.GAS)
			.version(7L)
			.build();

		SamplingItemSnapshot item = new SamplingItemSnapshot(
			31L, 32L, "NOX", "질소산화물", "NOx",
			MeasurementField.AIR, null, null, null, null,
			null, new BigDecimal("150"), false,
			AnalysisResult.empty().applyAnalysisResult(new BigDecimal("12.5"), "ppm", null, null));

		return new ScheduleSnapshot(
			"11", 11L, 1L, 5L,
			client,
			new TenantSnapshot(1L, "고객사", "333-33-33333", "대표", "도로명", "상세", "11111", "분석자", "책임자"),
			new TeamSnapshot(33L, "1팀", "멘토", "멘티", List.of()),
			new SamplingSnapshot(LocalTime.of(9, 30), LocalTime.of(11, 0), "관리자", "입회자", List.of(sheet)),
			List.of(item));
	}

	@Test
	void 스냅샷_트리를_끝까지_옮긴다() {
		ScheduleSnapshotResponse response = mapper.toSnapshotResponse(snapshot());

		assertThat(response.team().teamName()).isEqualTo("1팀");
		assertThat(response.tenant().name()).isEqualTo("고객사");
		assertThat(response.client().workplace().stack().stackId()).isEqualTo(22L);
		assertThat(response.client().workplace().stack().standardOxygen()).isEqualTo(6);
		assertThat(response.client().workplace().stack().facilities())
			.singleElement()
			.satisfies(facility -> assertThat(facility.name()).isEqualTo("보일러"));
	}

	@Test
	void 성적서_서명란_담당자는_고객사_스냅샷으로_나간다() {
		ScheduleSnapshotResponse response = mapper.toSnapshotResponse(snapshot());

		assertThat(response.tenant().analyst()).isEqualTo("분석자");
		assertThat(response.tenant().technicalManager()).isEqualTo("책임자");
	}

	@Test
	void 채취_시각과_현장_담당자는_채취_정보로_나간다() {
		ScheduleSnapshotResponse response = mapper.toSnapshotResponse(snapshot());

		assertThat(response.samplingData().samplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
		assertThat(response.samplingData().samplingEndedAt()).isEqualTo(LocalTime.of(11, 0));
		assertThat(response.samplingData().facilityManager()).isEqualTo("관리자");
		assertThat(response.samplingData().samplingWitness()).isEqualTo("입회자");
	}

	@Test
	void 실험분석_결과는_측정항목_안에_담겨_나간다() {
		ScheduleSnapshotResponse response = mapper.toSnapshotResponse(snapshot());

		assertThat(response.items())
			.singleElement()
			.satisfies(item -> {
				assertThat(item.allowance()).isEqualByComparingTo("150");
				assertThat(item.analysis().analysisValue()).isEqualByComparingTo("12.5");
				assertThat(item.analysis().unit()).isEqualTo("ppm");
			});
	}

	@Test
	void 시트_버전은_그대로_내려간다() {
		ScheduleSnapshotResponse response = mapper.toSnapshotResponse(snapshot());

		assertThat(response.samplingData().sheets())
			.singleElement()
			.satisfies(sheet -> {
				assertThat(sheet.getCategory()).isEqualTo(MeasurementCategory.GAS);
				assertThat(sheet.getVersion()).isEqualTo(7L);
			});
	}

	@Test
	void 메타와_겹치는_값과_저장_메타는_응답에_실리지_않는다() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

		JsonNode root = objectMapper.readTree(
			objectMapper.writeValueAsString(mapper.toSnapshotResponse(snapshot())));

		// 상태·관리번호·일자는 최상위 메타에만, 장비·기록지는 각각 team·samplingData 아래에만 있다.
		assertThat(root.fieldNames()).toIterable()
			.containsExactlyInAnyOrder("team", "tenant", "client", "samplingData", "items");
		assertThat(root.get("samplingData").get("sheets").get(0).get("version").asLong()).isEqualTo(7L);
	}

	@Test
	void null_스냅샷은_null로_돌려준다() {
		assertThat(mapper.toSnapshotResponse(null)).isNull();
	}

	/**
	 * 성적서 기본정보 일자 넷은 메타가 진실이라 <b>최상위</b>에 실린다.
	 * 문서에서 사본을 걷어낸 뒤 읽는 경로가 함께 사라졌던 적이 있어(쓰기 전용 필드가 됨) 고정해 둔다.
	 */
	@Test
	void 성적서_진행_일자는_최상위_응답에_실린다() {
		Schedule meta = Schedule.builder()
			.id(11L).tenantId(1L).stackId(10L).teamId(20L)
			.measurementField(MeasurementField.AIR)
			.status(ScheduleStatus.ANALYZING)
			.referenceNumber("2026-A-001")
			.schedulePurpose("자가측정용")
			.sampledAt(LocalDate.of(2026, 5, 1))
			.receivedAt(LocalDate.of(2026, 5, 2))
			.analyzedAt(LocalDate.of(2026, 5, 4))
			.issuedAt(LocalDate.of(2026, 5, 8))
			.build();

		ScheduleResponse response = mapper.toResponse(new ScheduleDetail(meta, snapshot()));

		assertThat(response.sampledAt()).isEqualTo(LocalDate.of(2026, 5, 1));
		assertThat(response.receivedAt()).isEqualTo(LocalDate.of(2026, 5, 2));
		assertThat(response.analyzedAt()).isEqualTo(LocalDate.of(2026, 5, 4));
		assertThat(response.issuedAt()).isEqualTo(LocalDate.of(2026, 5, 8));
		assertThat(response.referenceNumber()).isEqualTo("2026-A-001");
		assertThat(response.schedulePurpose()).isEqualTo("자가측정용");
	}
}
