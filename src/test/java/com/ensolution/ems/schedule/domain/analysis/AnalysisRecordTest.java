package com.ensolution.ems.schedule.domain.analysis;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실험분석정보가 들고 있는 판정 근거(허용기준치·산소보정)의 갱신 규칙과,
 * 실험·분석 탭·성적서 탭이 필드를 나눠 소유한다는 계약을 검증한다.
 * 실험실 입력값 수정으로는 근거가 흔들리지 않고, 회차의 측정항목이 정정될 때만 따라간다.
 */
class AnalysisRecordTest {

	private static SamplingItemSnapshot item(BigDecimal allowance, boolean oxygenApplicable) {
		return new SamplingItemSnapshot(
			101L, 1L, "TSP", "먼지", "Dust",
			MeasurementField.AIR, MeasurementMethod.DUST, PollutantPhase.PARTICLE,
			"입자상 채취기", "ES 01301", MeasurementCycle.MONTHLY, allowance, oxygenApplicable);
	}

	private static AnalysisRecord registered() {
		return AnalysisRecord.register(
			1L, 1L, item(new BigDecimal("30"), false),
			new BigDecimal("25"), "mg/Sm³", "중량법", "저울");
	}

	@Test
	@DisplayName("실험실 입력값 수정은 판정 근거를 바꾸지 않는다 — 원장 개정이 과거 회차로 흘러들면 안 된다")
	void updateKeepsJudgementBasis() {
		AnalysisRecord updated = registered().update(new BigDecimal("28"), null, null, null);

		assertThat(updated.getAllowance()).isEqualByComparingTo("30");
		assertThat(updated.isOxygenApplicable()).isFalse();
	}

	@Test
	@DisplayName("측정항목이 정정되면 판정 근거가 그 값을 따라간다 — 한 회차에서 기준이 둘로 갈라지면 안 된다")
	void syncFollowsCorrectedItem() {
		AnalysisRecord synced = registered().syncJudgementBasis(item(new BigDecimal("45"), true));

		assertThat(synced.getAllowance()).isEqualByComparingTo("45");
		assertThat(synced.isOxygenApplicable()).isTrue();
	}

	@Test
	@DisplayName("판정 근거를 맞출 때 실험실 입력값은 건드리지 않는다 — 기준이 바뀌었을 뿐 분석 결과는 그대로다")
	void syncKeepsLabValues() {
		AnalysisRecord synced = registered().syncJudgementBasis(item(null, true));

		assertThat(synced.getAnalysisValue()).isEqualByComparingTo("25");
		assertThat(synced.getUnit()).isEqualTo("mg/Sm³");
		assertThat(synced.getAnalysisMethod()).isEqualTo("중량법");
		assertThat(synced.getAllowance()).isNull();
	}

	// ===== 채취시간 — 성적서 탭이 소유하는 필드 =====

	private static AnalysisRecord withTimes() {
		return registered().applySamplingTime(LocalTime.of(9, 30), LocalTime.of(10, 0));
	}

	@Test
	@DisplayName("채취시간만 가진 기록을 만들 수 있다 — 성적서 작성이 실험실 분석보다 먼저 올 수 있다")
	void ofSamplingTimeCreatesRecordWithoutLabValues() {
		AnalysisRecord record = AnalysisRecord.ofSamplingTime(
			1L, 1L, item(new BigDecimal("30"), true), LocalTime.of(9, 30), LocalTime.of(10, 0));

		assertThat(record.getSamplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
		assertThat(record.getSamplingEndedAt()).isEqualTo(LocalTime.of(10, 0));
		assertThat(record.getAnalysisValue()).isNull();
		// 어느 경로로 만들어졌든 판정 근거는 스냅샷에서 복사한다
		assertThat(record.getAllowance()).isEqualByComparingTo("30");
		assertThat(record.isOxygenApplicable()).isTrue();
	}

	@Test
	@DisplayName("채취시간 저장은 실험실 입력값을 건드리지 않는다 — 실험·분석 탭이 소유하는 필드다")
	void applySamplingTimeKeepsLabValues() {
		AnalysisRecord record = withTimes();

		assertThat(record.getAnalysisValue()).isEqualByComparingTo("25");
		assertThat(record.getUnit()).isEqualTo("mg/Sm³");
		assertThat(record.getAnalysisMethod()).isEqualTo("중량법");
		assertThat(record.getAnalysisEquipment()).isEqualTo("저울");
	}

	@Test
	@DisplayName("채취시간은 전달값을 그대로 채택한다 — 빈 값을 넘기면 지워져야 다시 비울 수 있다")
	void applySamplingTimeAdoptsNullAsCleared() {
		AnalysisRecord cleared = withTimes().applySamplingTime(null, null);

		assertThat(cleared.getSamplingStartedAt()).isNull();
		assertThat(cleared.getSamplingEndedAt()).isNull();
		assertThat(cleared.getAnalysisValue()).isEqualByComparingTo("25");
	}

	@Test
	@DisplayName("자정을 넘기는 채취(23:00→01:00)도 그대로 저장된다 — 순서를 검증하면 정상 입력을 막는다")
	void applySamplingTimeAllowsOvernightWindow() {
		AnalysisRecord overnight = registered().applySamplingTime(LocalTime.of(23, 0), LocalTime.of(1, 0));

		assertThat(overnight.getSamplingStartedAt()).isEqualTo(LocalTime.of(23, 0));
		assertThat(overnight.getSamplingEndedAt()).isEqualTo(LocalTime.of(1, 0));
	}

	@Test
	@DisplayName("실험실 입력값 수정은 채취시간을 건드리지 않는다 — 성적서 탭이 소유하는 필드다")
	void updateKeepsSamplingTime() {
		AnalysisRecord updated = withTimes().update(new BigDecimal("28"), "ppm", "흡광법", "분광기");

		assertThat(updated.getSamplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
		assertThat(updated.getSamplingEndedAt()).isEqualTo(LocalTime.of(10, 0));
	}

	@Test
	@DisplayName("판정 근거를 맞출 때도 채취시간은 그대로다")
	void syncKeepsSamplingTime() {
		AnalysisRecord synced = withTimes().syncJudgementBasis(item(new BigDecimal("45"), true));

		assertThat(synced.getSamplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
		assertThat(synced.getSamplingEndedAt()).isEqualTo(LocalTime.of(10, 0));
	}
}
