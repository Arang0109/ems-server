package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.FakeAnalysisRecordRepository;
import com.ensolution.ems.schedule.application.FakeScheduleDocumentRepository;
import com.ensolution.ems.schedule.application.command.export.SamplingItemExportView;
import com.ensolution.ems.schedule.application.command.export.ScheduleExportView;
import com.ensolution.ems.schedule.application.mapper.ScheduleExportViewMapper;
import com.ensolution.ems.schedule.application.mapper.SheetExportViewMapper;
import com.ensolution.ems.schedule.domain.analysis.AnalysisRecord;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * 내보내기 뷰 조립 검증.
 * 측정계획 문서와 실험분석정보는 서로 다른 컬렉션에 있고, 읽는 쪽에서 {@code pollutantId}로
 * 다시 합쳐야 성적서에 분석값이 실린다 — 그 결합이 이 조립부의 계약이다.
 */
class ScheduleExportAssemblerTest {

	private static final Long TENANT_ID = 10L;
	private static final Long SCHEDULE_ID = 1L;

	private FakeScheduleDocumentRepository documentRepository;
	private FakeAnalysisRecordRepository analysisRepository;
	private ScheduleExportAssembler assembler;

	@BeforeEach
	void setUp() {
		documentRepository = new FakeScheduleDocumentRepository();
		analysisRepository = new FakeAnalysisRecordRepository();
		assembler = new ScheduleExportAssembler(
			documentRepository,
			new AnalysisRecordIndexer(analysisRepository),
			new ScheduleExportViewMapper(new SheetExportViewMapper()));
	}

	private SamplingItemSnapshot item(Long pollutantId, String nameKr, String allowance) {
		return new SamplingItemSnapshot(
			pollutantId * 10, pollutantId, "CODE-" + pollutantId, nameKr, null,
			null, null, null, null, null,
			MeasurementCycle.QUARTERLY, allowance == null ? null : new BigDecimal(allowance), true);
	}

	private void givenDocument(List<SamplingItemSnapshot> items) {
		documentRepository.save(new ScheduleSnapshot(
			String.valueOf(SCHEDULE_ID), SCHEDULE_ID, TENANT_ID,
			null, null, null, null, null, null, items, null, null, null));
	}

	private void givenAnalysis(Long pollutantId, String value, String unit) {
		analysisRepository.save(AnalysisRecord.builder()
			.tenantId(TENANT_ID)
			.scheduleId(SCHEDULE_ID)
			.pollutantId(pollutantId)
			.analysisValue(new BigDecimal(value))
			.unit(unit)
			.analysisMethod("분석방법-" + pollutantId)
			.analysisEquipment("분석장비-" + pollutantId)
			.build());
	}

	@Test
	void 문서와_실험분석정보가_측정물질로_결합되어_뷰에_실린다() {
		givenDocument(List.of(item(1L, "먼지", "50"), item(2L, "질소산화물", "200")));
		givenAnalysis(1L, "12.5", "mg/Sm3");
		givenAnalysis(2L, "30", "ppm");

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
		givenDocument(List.of(item(1L, "먼지", "50")));

		ScheduleExportView view = assembler.assemble(SCHEDULE_ID, TENANT_ID);

		assertThat(view.getItems()).hasSize(1);
		assertThat(view.getItems().getFirst().getName()).isEqualTo("먼지");
		assertThat(view.getItems().getFirst().getAnalysisValue()).isNull();
	}

	@Test
	void 다른_계획의_분석_결과는_섞이지_않는다() {
		givenDocument(List.of(item(1L, "먼지", "50")));
		analysisRepository.save(AnalysisRecord.builder()
			.tenantId(TENANT_ID)
			.scheduleId(999L)
			.pollutantId(1L)
			.analysisValue(new BigDecimal("99"))
			.build());

		ScheduleExportView view = assembler.assemble(SCHEDULE_ID, TENANT_ID);

		assertThat(view.getItems().getFirst().getAnalysisValue()).isNull();
	}

	@Test
	void 문서가_없으면_조회_예외가_그대로_올라간다() {
		assertThatThrownBy(() -> assembler.assemble(SCHEDULE_ID, TENANT_ID))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_DOCUMENT_NOT_FOUND);
	}
}
