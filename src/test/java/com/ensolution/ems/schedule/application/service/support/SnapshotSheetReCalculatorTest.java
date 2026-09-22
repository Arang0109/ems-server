package com.ensolution.ems.schedule.application.service.support;

import com.ensolution.ems.equipment.domain.spec.NozzleSpec;
import com.ensolution.ems.equipment.domain.spec.PitotTubeSpec;
import com.ensolution.ems.global.common.enums.MeasurementMode;
import com.ensolution.ems.global.common.enums.Shape;
import com.ensolution.ems.schedule.application.calculation.SamplingItemInput;
import com.ensolution.ems.schedule.application.calculation.SheetCalculator;
import com.ensolution.ems.schedule.application.calculation.StackData;
import com.ensolution.ems.schedule.domain.sampling.MeasurementCategory;
import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;
import com.ensolution.ems.schedule.domain.snapshot.ClientSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.StackSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.WorkplaceSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 재계산기가 스냅샷에서 <b>측정항목 입력</b>(등속흡인 여부·표준 채취시간)을 올바르게 뽑아 계산 엔진에 넘기는지 고정한다.
 * 계산 엔진은 스냅샷을 모르므로, 카탈로그 측정방식(먼지·중금속·수은)이 곧 등속흡인이라는 판정은 여기서만 일어난다.
 * 측정방식 도입 이전 문서(mode null)는 등속흡인이 아니어야 옛 회차의 가스상 행을 건드리지 않는다.
 */
class SnapshotSheetReCalculatorTest {

	/** 계산은 하지 않고 넘어온 항목 입력만 붙잡아 두는 스텁. 스텝을 돌리지 않으므로 시트를 그대로 돌려준다. */
	private static class CapturingCalculator extends SheetCalculator {
		final List<List<SamplingItemInput>> captured = new ArrayList<>();

		CapturingCalculator() {
			super(List.of());
		}

		@Override
		public SamplingSheet calculate(SamplingSheet sheet, StackData stackData,
		                               List<PitotTubeSpec.PitotCoefficient> pitotCoefficients,
		                               List<NozzleSpec.NozzleDiameter> diameters, BigDecimal deltaH,
		                               List<SamplingItemInput> items) {
			captured.add(items);
			return sheet;
		}
	}

	private static SamplingItemSnapshot item(Long pollutantId, MeasurementMode mode, Integer samplingMinutes) {
		return new SamplingItemSnapshot(pollutantId * 10, pollutantId, null, "물질" + pollutantId, null,
			null, null, null, mode, null, null, samplingMinutes, null, null, null, false, null);
	}

	/** 계산 입력이 있으려면 측정시설 스냅샷이 있어야 한다 — 없으면 재계산기가 조기 반환한다. */
	private static ScheduleSnapshot snapshot(List<SamplingItemSnapshot> items) {
		StackSnapshot stack = new StackSnapshot(1L, null, "굴뚝", null, null, null, 4,
			10.0, 1.0, null, Shape.CIRCULAR, null, null, null);
		WorkplaceSnapshot workplace = new WorkplaceSnapshot(1L, "사업장", null, null, null, null, null, null, stack);
		ClientSnapshot client = new ClientSnapshot(1L, "의뢰기관", null, null, null, null, null, null, null, workplace);
		return new ScheduleSnapshot("1", 1L, 1L, null, client, null, null, null, items, null);
	}

	private static List<SamplingSheet> sheets() {
		return List.of(SamplingSheet.builder().category(MeasurementCategory.HEAVY_METAL).build());
	}

	@Test
	void 먼지_중금속_수은_항목만_등속흡인으로_표시되고_채취시간은_그대로_옮긴다() {
		CapturingCalculator calculator = new CapturingCalculator();
		ScheduleSnapshot snapshot = snapshot(List.of(
			item(1L, MeasurementMode.HEAVY_METAL, null),   // 비소화합물
			item(2L, MeasurementMode.GAS_SAMPLING, 30),    // SO2 흡수액
			item(3L, MeasurementMode.DUST, null),
			item(4L, MeasurementMode.MERCURY, null),
			item(5L, MeasurementMode.DIRECT_READING, null)));

		new SnapshotSheetReCalculator(calculator).reCalculate(snapshot, sheets());

		assertThat(calculator.captured).hasSize(1);
		assertThat(calculator.captured.getFirst()).containsExactly(
			new SamplingItemInput(1L, MeasurementCategory.HEAVY_METAL, null),
			new SamplingItemInput(2L, null, 30),
			new SamplingItemInput(3L, MeasurementCategory.DUST, null),
			new SamplingItemInput(4L, MeasurementCategory.MERCURY, null),
			new SamplingItemInput(5L, null, null));
	}

	@Test
	void 측정방식이_없는_구_문서_항목은_등속흡인이_아니다() {
		CapturingCalculator calculator = new CapturingCalculator();

		new SnapshotSheetReCalculator(calculator).reCalculate(snapshot(List.of(item(1L, null, 20))), sheets());

		assertThat(calculator.captured.getFirst()).containsExactly(new SamplingItemInput(1L, null, 20));
	}

	@Test
	void 항목이_없는_문서는_빈_목록을_넘긴다() {
		CapturingCalculator calculator = new CapturingCalculator();

		new SnapshotSheetReCalculator(calculator).reCalculate(snapshot(null), sheets());

		assertThat(calculator.captured.getFirst()).isEmpty();
	}
}
