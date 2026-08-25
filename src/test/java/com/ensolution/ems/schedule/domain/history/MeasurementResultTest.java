package com.ensolution.ems.schedule.domain.history;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.schedule.domain.analysis.AnalysisRecord;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 허용기준 초과 판정 검증.
 * 산소보정을 적용하는 항목은 보정 후 농도로 견줘야 한다는 것과, 판정할 수 없을 때 false로 단정하지 않는다는
 * 것이 핵심 계약이다.
 */
class MeasurementResultTest {

	private static final BigDecimal RAW = new BigDecimal("120");
	private static final BigDecimal CORRECTED = new BigDecimal("80");
	private static final BigDecimal ALLOWANCE = new BigDecimal("100");

	@Test
	@DisplayName("산소보정 항목은 보정 후 농도로 판정한다")
	void judgesByCorrectedWhenOxygenApplicable() {
		MeasurementResult result = MeasurementResult.of(RAW, "ppm", CORRECTED, null, ALLOWANCE, true);

		// 실측은 기준을 넘지만 보정하면 기준 이내다
		assertThat(result.exceeded()).isFalse();
	}

	@Test
	@DisplayName("산소보정을 적용하지 않는 항목은 실측 농도로 판정한다")
	void judgesByRawWhenNotApplicable() {
		MeasurementResult result = MeasurementResult.of(RAW, "ppm", CORRECTED, null, ALLOWANCE, false);

		assertThat(result.exceeded()).isTrue();
	}

	@Test
	@DisplayName("기준과 같은 값은 초과가 아니다")
	void equalIsNotExceeded() {
		MeasurementResult result = MeasurementResult.of(ALLOWANCE, "ppm", null, null, ALLOWANCE, false);

		assertThat(result.exceeded()).isFalse();
	}

	@Test
	@DisplayName("허용기준이 없으면 판정하지 않는다 — false로 두면 기준 이내로 잘못 읽힌다")
	void doesNotJudgeWithoutAllowance() {
		MeasurementResult result = MeasurementResult.of(RAW, "ppm", CORRECTED, null, null, true);

		assertThat(result.exceeded()).isNull();
	}

	@Test
	@DisplayName("보정 농도가 없는 산소보정 항목은 판정하지 않는다")
	void doesNotJudgeWithoutComparedValue() {
		MeasurementResult result = MeasurementResult.of(RAW, "ppm", null, null, ALLOWANCE, true);

		assertThat(result.exceeded()).isNull();
	}

	@Test
	@DisplayName("결과값이 없는 이행은 빈 값으로 표현한다")
	void empty() {
		MeasurementResult empty = MeasurementResult.empty();

		assertThat(empty.hasValue()).isFalse();
		assertThat(empty.concentration()).isNull();
		assertThat(empty.exceeded()).isNull();
	}

	@Test
	@DisplayName("완료 시점에 분석 결과가 없으면 허용기준만 남기고 판정하지 않는다")
	void ofCompletionWithoutAnalysis() {
		MeasurementResult result = MeasurementResult.ofCompletion(item(false), null);

		assertThat(result.hasValue()).isFalse();
		assertThat(result.unit()).isNull();
		assertThat(result.exceeded()).isNull();
		// 현장 측정은 이행했으므로 판정 근거는 남는다
		assertThat(result.allowance()).isEqualByComparingTo(ALLOWANCE);
	}

	@Test
	@DisplayName("실측값·단위는 분석 결과에서, 허용기준은 측정항목 스냅샷에서 가져온다")
	void ofCompletionTakesAllowanceFromSnapshot() {
		SamplingItemSnapshot item = item(false);
		AnalysisRecord analysis = analysis(item, RAW, "ppm")
			.toBuilder()
			.allowance(new BigDecimal("999"))
			.build();

		MeasurementResult result = MeasurementResult.ofCompletion(item, analysis);

		assertThat(result.concentration()).isEqualByComparingTo(RAW);
		assertThat(result.unit()).isEqualTo("ppm");
		// 분석 기록에도 사본이 있지만 이행 기록의 다른 값과 출처를 맞춘다
		assertThat(result.allowance()).isEqualByComparingTo(ALLOWANCE);
		assertThat(result.exceeded()).isTrue();
	}

	@Test
	@DisplayName("보정 농도와 배출량은 아직 채우지 않으므로 산소보정 항목은 판정이 보류된다")
	void ofCompletionLeavesDerivedValuesEmpty() {
		SamplingItemSnapshot item = item(true);

		MeasurementResult result = MeasurementResult.ofCompletion(item, analysis(item, RAW, "ppm"));

		assertThat(result.correctedConcentration()).isNull();
		assertThat(result.emission()).isNull();
		// 실측은 기준을 넘지만 비교 대상인 보정 농도가 없다
		assertThat(result.exceeded()).isNull();
	}

	private static SamplingItemSnapshot item(boolean oxygenApplicable) {
		return new SamplingItemSnapshot(
			1L, 11L, "NOX", "질소산화물", "NOx",
			MeasurementField.AIR, null, null, null, null,
			MeasurementCycle.QUARTERLY, ALLOWANCE, oxygenApplicable);
	}

	private static AnalysisRecord analysis(SamplingItemSnapshot item, BigDecimal value, String unit) {
		return AnalysisRecord.register(10L, 1L, item, value, unit, "주시험법", "장비");
	}
}
