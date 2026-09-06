package com.ensolution.ems.schedule.application.service.assembler;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.FakeScheduleDocumentRepository;
import com.ensolution.ems.schedule.application.FakeScheduleRepository;
import com.ensolution.ems.schedule.application.command.export.SamplingItemExportView;
import com.ensolution.ems.schedule.application.command.export.ScheduleExportView;
import com.ensolution.ems.schedule.application.mapper.ScheduleExportViewMapper;
import com.ensolution.ems.schedule.application.mapper.SheetExportViewMapper;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.domain.snapshot.AnalysisResult;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * 내보내기 뷰 조립 검증.
 *
 * <p>성적서 한 장에 필요한 데이터가 두 저장소에 나뉘어 있다 — 기본정보 표(관리번호·측정분야·일자)는
 * 메타(MySQL)가, 나머지는 세부 문서(MongoDB)가 갖는다. 문서에 메타의 사본을 두지 않으므로
 * <b>읽는 쪽에서 합쳐야</b> 성적서가 완성된다. 그 결합이 이 조립부의 계약이다.
 */
class ScheduleExportAssemblerTest {

	private static final Long TENANT_ID = 10L;
	private static final Long SCHEDULE_ID = 1L;

	private FakeScheduleRepository scheduleRepository;
	private FakeScheduleDocumentRepository documentRepository;
	private ScheduleExportAssembler assembler;

	@BeforeEach
	void setUp() {
		scheduleRepository = new FakeScheduleRepository();
		documentRepository = new FakeScheduleDocumentRepository();
		assembler = new ScheduleExportAssembler(
			scheduleRepository,
			documentRepository,
			new ScheduleExportViewMapper(new SheetExportViewMapper()));
	}

	private void givenSchedule() {
		scheduleRepository.given(Schedule.builder()
			.id(SCHEDULE_ID)
			.tenantId(TENANT_ID)
			.stackId(100L)
			.teamId(200L)
			.measurementField(MeasurementField.AIR)
			.schedulePurpose("자가측정용")
			.referenceNumber("2026-A-001")
			.sampledAt(LocalDate.of(2026, 5, 1))
			.receivedAt(LocalDate.of(2026, 5, 2))
			.issuedAt(LocalDate.of(2026, 5, 8))
			.status(ScheduleStatus.REPORT_COMPLETED)
			.build());
	}

	private SamplingItemSnapshot item(Long pollutantId, String nameKr, String allowance) {
		return new SamplingItemSnapshot(
			pollutantId * 10, pollutantId, "CODE-" + pollutantId, nameKr, null,
			null, null, null, null, null,
			MeasurementCycle.QUARTERLY, allowance == null ? null : new BigDecimal(allowance), true, null);
	}

	private SamplingItemSnapshot analyzed(SamplingItemSnapshot item, String value, String unit) {
		return item.withAnalysis(AnalysisResult.empty().applyAnalysisResult(
			new BigDecimal(value), unit, "분석방법-" + item.pollutantId(), "분석장비-" + item.pollutantId()));
	}

	private void givenDocument(List<SamplingItemSnapshot> items) {
		documentRepository.given(new ScheduleSnapshot(
			String.valueOf(SCHEDULE_ID), SCHEDULE_ID, TENANT_ID, 0L, null, null, null, null, items));
	}

	@Test
	void 성적서_기본정보는_메타에서_가져온다() {
		givenSchedule();
		givenDocument(List.of(item(1L, "먼지", "50")));

		ScheduleExportView view = assembler.assemble(SCHEDULE_ID, TENANT_ID);

		assertThat(view.getReferenceNumber()).isEqualTo("2026-A-001");
		assertThat(view.getSchedulePurpose()).isEqualTo("자가측정용");
		assertThat(view.getSampledAt()).isEqualTo(LocalDate.of(2026, 5, 1));
		assertThat(view.getReceivedAt()).isEqualTo(LocalDate.of(2026, 5, 2));
		assertThat(view.getIssuedAt()).isEqualTo(LocalDate.of(2026, 5, 8));
	}

	@Test
	void 측정항목에_담긴_분석_결과가_그대로_뷰에_실린다() {
		givenSchedule();
		givenDocument(List.of(
			analyzed(item(1L, "먼지", "50"), "12.5", "mg/Sm3"),
			analyzed(item(2L, "질소산화물", "200"), "30", "ppm")));

		ScheduleExportView view = assembler.assemble(SCHEDULE_ID, TENANT_ID);

		assertThat(view.getItems())
			.extracting(SamplingItemExportView::getName,
				SamplingItemExportView::getAnalysisValue,
				SamplingItemExportView::getUnit)
			.containsExactly(
				tuple("먼지", new BigDecimal("12.5"), "mg/Sm3"),
				tuple("질소산화물", new BigDecimal("30"), "ppm"));
	}

	@Test
	void 분석_결과가_하나도_없어도_조립에_성공한다() {
		// 실험실 입력 전인 계획도 성적서 양식은 뽑을 수 있어야 한다
		givenSchedule();
		givenDocument(List.of(item(1L, "먼지", "50")));

		ScheduleExportView view = assembler.assemble(SCHEDULE_ID, TENANT_ID);

		assertThat(view.getItems()).hasSize(1);
		assertThat(view.getItems().getFirst().getName()).isEqualTo("먼지");
		assertThat(view.getItems().getFirst().getAnalysisValue()).isNull();
	}

	@Test
	void 메타가_없으면_조회_예외가_그대로_올라간다() {
		assertThatThrownBy(() -> assembler.assemble(SCHEDULE_ID, TENANT_ID))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_FOUND);
	}

	@Test
	void 문서가_없으면_조회_예외가_그대로_올라간다() {
		givenSchedule();

		assertThatThrownBy(() -> assembler.assemble(SCHEDULE_ID, TENANT_ID))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_DOCUMENT_NOT_FOUND);
	}

	@Test
	void 다른_고객사의_계획은_찾지_못한다() {
		givenSchedule();
		givenDocument(List.of(item(1L, "먼지", "50")));

		assertThatThrownBy(() -> assembler.assemble(SCHEDULE_ID, 999L))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_FOUND);
	}
}
