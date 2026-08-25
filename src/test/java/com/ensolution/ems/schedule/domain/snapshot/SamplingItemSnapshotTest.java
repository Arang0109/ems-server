package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;
import com.ensolution.ems.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 측정항목 스냅샷의 정정 규칙 검증.
 * <p>
 * 계획을 세운 뒤 현장에서 허용기준·산소보정이 실제와 다름을 확인했을 때 그 회차 문서를 고치는
 * 경로다. 원장 변경이 과거 회차로 흘러드는 통로가 아니라는 것과, 정정이 대상 항목 하나에만
 * 미친다는 것이 핵심 계약이다.
 */
class SamplingItemSnapshotTest {

	private static SamplingItemSnapshot item(Long pollutantId, String nameKr, BigDecimal allowance) {
		return new SamplingItemSnapshot(
			pollutantId * 100, pollutantId, "CODE-" + pollutantId, nameKr, nameKr,
			MeasurementField.AIR, MeasurementMethod.FIELD_MEASUREMENT, PollutantPhase.GAS,
			"가스분석기", "ES 01310", MeasurementCycle.MONTHLY, allowance, false);
	}

	@Nested
	@DisplayName("applyCondition")
	class ApplyCondition {

		@Test
		@DisplayName("허용기준은 전달값을 그대로 채택한다 — null 로 비울 수 있어야 '미지정'과 0을 구분한다")
		void adoptsAllowanceAsGiven() {
			SamplingItemSnapshot corrected = item(1L, "먼지", new BigDecimal("30"))
				.applyCondition(MeasurementCycle.MONTHLY, null, false);

			assertThat(corrected.allowance()).isNull();
		}

		@Test
		@DisplayName("산소보정 해제가 반영된다 — 해제가 곧 false 라 '유지'로 읽을 자리가 없다")
		void clearsOxygenApplicable() {
			SamplingItemSnapshot applied = item(1L, "먼지", new BigDecimal("30"))
				.applyCondition(MeasurementCycle.MONTHLY, new BigDecimal("30"), true);

			assertThat(applied.applyCondition(MeasurementCycle.MONTHLY, new BigDecimal("30"), false)
				.oxygenApplicable()).isFalse();
		}

		@Test
		@DisplayName("측정주기는 null 이면 기존 값을 유지한다")
		void keepsCycleWhenNotGiven() {
			SamplingItemSnapshot corrected = item(1L, "먼지", new BigDecimal("30"))
				.applyCondition(null, new BigDecimal("45"), false);

			assertThat(corrected.cycle()).isEqualTo(MeasurementCycle.MONTHLY);
		}

		@Test
		@DisplayName("어느 물질인지와 원장 연결키는 바뀌지 않는다 — 그것이 바뀌면 다른 항목이지 정정이 아니다")
		void keepsIdentity() {
			SamplingItemSnapshot origin = item(1L, "먼지", new BigDecimal("30"));

			SamplingItemSnapshot corrected =
				origin.applyCondition(MeasurementCycle.ANNUAL, new BigDecimal("45"), true);

			assertThat(corrected.pollutantId()).isEqualTo(origin.pollutantId());
			assertThat(corrected.stackPollutantId()).isEqualTo(origin.stackPollutantId());
			assertThat(corrected.code()).isEqualTo(origin.code());
			assertThat(corrected.nameKr()).isEqualTo(origin.nameKr());
		}
	}

	@Nested
	@DisplayName("ScheduleSnapshot.withItemReplaced")
	class WithItemReplaced {

		private ScheduleSnapshot snapshotOf(List<SamplingItemSnapshot> items) {
			return new ScheduleSnapshot(
				"1", 1L, 1L, null, null, null, null, null, List.of(), items, List.of(), 0L, null);
		}

		@Test
		@DisplayName("대상 항목만 갈아끼우고 나머지 항목과 순서는 그대로 둔다")
		void replacesOnlyTarget() {
			SamplingItemSnapshot dust = item(1L, "먼지", new BigDecimal("30"));
			SamplingItemSnapshot nox = item(2L, "질소산화물", new BigDecimal("150"));
			ScheduleSnapshot snapshot = snapshotOf(List.of(dust, nox));

			ScheduleSnapshot changed = snapshot.withItemReplaced(
				2L, nox.applyCondition(MeasurementCycle.MONTHLY, new BigDecimal("200"), false));

			assertThat(changed.items()).hasSize(2);
			assertThat(changed.items().get(0)).isEqualTo(dust);
			assertThat(changed.items().get(1).allowance()).isEqualByComparingTo("200");
		}

		@Test
		@DisplayName("계획에 없는 물질은 requireItem 이 먼저 거부한다")
		void rejectsUnknownPollutant() {
			ScheduleSnapshot snapshot = snapshotOf(List.of(item(1L, "먼지", new BigDecimal("30"))));

			assertThatThrownBy(() -> snapshot.requireItem(9L)).isInstanceOf(CustomException.class);
		}
	}
}
