package com.ensolution.ems.schedule.application.command.export;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 채취기록부 최상위 변수 표를 고정한다. 이름은 고객 템플릿의 계약이고, 각 변수의 값 추출은 하위 뷰 non-null 계약
 * 아래에서 예외 없이 동작해야 한다. 목록 변수는 검사기가 반복 변수를 바인딩할 원소 타입을 알아야 한다.
 */
class SamplingRecordVariableTest {

	private static final ScheduleExportView PLAN = ScheduleExportView.builder()
		.referenceNumber("REF-1")
		.items(List.of())
		.customFields(Map.of("siteCode", "A-01"))
		.build();

	/** 매퍼가 만드는 모양 — 하위 뷰는 항상 채워져 있다. */
	private static final SheetExportView SHEET = SheetExportView.builder()
		.category("먼지")
		.weather(WeatherExportView.builder().build())
		.moisture(MoistureExportView.builder().build())
		.gas(GasExportView.builder().build())
		.flow(FlowExportView.builder().build())
		.particle(ParticleExportView.builder().build())
		.points(List.of())
		.gaseousSamplings(List.of())
		.build();

	@Test
	void 이름으로_변수를_찾는다() {
		assertThat(SamplingRecordVariable.byName("custom")).contains(SamplingRecordVariable.CUSTOM);
		assertThat(SamplingRecordVariable.byName("plan")).contains(SamplingRecordVariable.PLAN);
		assertThat(SamplingRecordVariable.byName("nope")).isEmpty();
	}

	@Test
	void 모든_변수가_하위_뷰_계약_아래에서_값을_돌려준다() {
		for (SamplingRecordVariable variable : SamplingRecordVariable.values()) {
			assertThat(variable.extract(PLAN, SHEET)).as(variable.getVariableName()).isNotNull();
		}
	}

	@Test
	void custom_은_계획의_커스텀_필드_맵이다() {
		assertThat(SamplingRecordVariable.CUSTOM.extract(PLAN, SHEET)).isEqualTo(Map.of("siteCode", "A-01"));
		assertThat(SamplingRecordVariable.CUSTOM.getType()).isEqualTo(Map.class);
	}

	@Test
	void 목록_변수는_원소_타입을_안다() {
		assertThat(SamplingRecordVariable.POINTS.isList()).isTrue();
		assertThat(SamplingRecordVariable.POINTS.getElementType()).isEqualTo(PointExportView.class);
		assertThat(SamplingRecordVariable.ITEMS.getElementType()).isEqualTo(SamplingItemExportView.class);
		assertThat(SamplingRecordVariable.GASEOUS_SAMPLINGS.getElementType()).isEqualTo(SampleExportView.class);
		assertThat(SamplingRecordVariable.PLAN.isList()).isFalse();
	}
}
