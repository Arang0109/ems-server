package com.ensolution.ems.schedule.domain;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.schedule.domain.sampling.SamplingPoint;
import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;
import com.ensolution.ems.schedule.domain.snapshot.SamplingSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스냅샷·메타 내용으로부터 진행 단계를 파생시키는 규칙 검증.
 * 핵심 계약은 두 가지다 — 전진만 하고 되돌아가지 않으며, 몇 번 호출해도 결과가 같다(멱등).
 * <p>
 * 근거가 두 저장소에 나뉘어 있다는 점도 함께 고정한다 — 채취 착수는 문서에서, 시료 접수는
 * 메타에서 읽는다. 접수일자는 성적서 진행 값이라 메타가 진실이기 때문이다.
 */
class ScheduleProgressTest {

	private static Schedule metaWith(ScheduleStatus status) {
		return metaWith(status, null);
	}

	private static Schedule metaWith(ScheduleStatus status, LocalDate receivedAt) {
		return Schedule.builder()
			.id(1L)
			.tenantId(10L)
			.stackId(100L)
			.teamId(200L)
			.measurementField(MeasurementField.AIR)
			.sampledAt(LocalDate.of(2026, 5, 1))
			.receivedAt(receivedAt)
			.status(status)
			.build();
	}

	private static ScheduleSnapshot snapshotWith(LocalTime samplingStartedAt, List<SamplingSheet> sheets) {
		SamplingSnapshot sampling = new SamplingSnapshot(samplingStartedAt, null, null, null, sheets);
		return new ScheduleSnapshot("1", 1L, 10L, null, null, null, null, sampling, null);
	}

	/** 측정점에 실측값(배출가스 온도)이 들어온 시트. */
	private static SamplingSheet sheetWithMeasuredValue() {
		return SamplingSheet.builder()
			.samplingPoints(List.of(SamplingPoint.builder().gasTemperature(new BigDecimal("120.5")).build()))
			.build();
	}

	/** 측정점은 있으나 실측값이 아직 비어 있는 시트(틀만 만들어 둔 상태). */
	private static SamplingSheet sheetWithEmptyPoints() {
		return SamplingSheet.builder()
			.samplingPoints(List.of(SamplingPoint.builder().build()))
			.build();
	}

	@Nested
	@DisplayName("측정 착수 전이")
	class MeasurementStart {

		@Test
		@DisplayName("채취 시작시각이 입력되면 측정 중으로 전진한다")
		void advancesOnSamplingStartTime() {
			Schedule meta = metaWith(ScheduleStatus.SCHEDULED);
			ScheduleSnapshot snapshot = snapshotWith(LocalTime.of(9, 30), null);

			assertThat(ScheduleProgress.advance(meta, snapshot).getStatus())
				.isEqualTo(ScheduleStatus.MEASURING);
		}

		@Test
		@DisplayName("측정점에 실측값이 들어오면 측정 중으로 전진한다")
		void advancesOnMeasuredValue() {
			Schedule meta = metaWith(ScheduleStatus.SCHEDULED);
			ScheduleSnapshot snapshot = snapshotWith(null, List.of(sheetWithMeasuredValue()));

			assertThat(ScheduleProgress.advance(meta, snapshot).getStatus())
				.isEqualTo(ScheduleStatus.MEASURING);
		}

		@Test
		@DisplayName("시트 틀만 저장한 상태는 측정 착수로 보지 않는다")
		void staysScheduledWhenPointsAreEmpty() {
			Schedule meta = metaWith(ScheduleStatus.SCHEDULED);
			ScheduleSnapshot snapshot = snapshotWith(null, List.of(sheetWithEmptyPoints()));

			assertThat(ScheduleProgress.advance(meta, snapshot).getStatus())
				.isEqualTo(ScheduleStatus.SCHEDULED);
		}

		@Test
		@DisplayName("시트가 없으면 전진하지 않는다")
		void staysScheduledWithoutSheets() {
			Schedule meta = metaWith(ScheduleStatus.SCHEDULED);
			ScheduleSnapshot snapshot = snapshotWith(null, null);

			assertThat(ScheduleProgress.advance(meta, snapshot).getStatus())
				.isEqualTo(ScheduleStatus.SCHEDULED);
		}

		@Test
		@DisplayName("채취 스냅샷이 아직 없어도 판정에 실패하지 않는다")
		void toleratesMissingSamplingSnapshot() {
			Schedule meta = metaWith(ScheduleStatus.SCHEDULED);
			ScheduleSnapshot snapshot = new ScheduleSnapshot("1", 1L, 10L, null, null, null, null, null, null);

			assertThat(ScheduleProgress.advance(meta, snapshot).getStatus())
				.isEqualTo(ScheduleStatus.SCHEDULED);
		}
	}

	@Nested
	@DisplayName("분석 착수 전이")
	class AnalysisStart {

		@Test
		@DisplayName("측정 중에 시료접수일자가 입력되면 분석 중으로 전진한다")
		void advancesOnReceivedAt() {
			Schedule meta = metaWith(ScheduleStatus.MEASURING, LocalDate.of(2026, 5, 2));
			ScheduleSnapshot snapshot = snapshotWith(null, null);

			assertThat(ScheduleProgress.advance(meta, snapshot).getStatus())
				.isEqualTo(ScheduleStatus.ANALYZING);
		}

		@Test
		@DisplayName("측정 예정에서 시트와 접수일자가 함께 들어오면 분석 중까지 두 단계 전진한다")
		void advancesTwoStepsAtOnce() {
			Schedule meta = metaWith(ScheduleStatus.SCHEDULED, LocalDate.of(2026, 5, 2));
			ScheduleSnapshot snapshot = snapshotWith(null, List.of(sheetWithMeasuredValue()));

			assertThat(ScheduleProgress.advance(meta, snapshot).getStatus())
				.isEqualTo(ScheduleStatus.ANALYZING);
		}

		@Test
		@DisplayName("측정에 착수하지 않은 채 접수일자만 들어오면 단계를 건너뛰지 않는다")
		void doesNotSkipMeasuring() {
			Schedule meta = metaWith(ScheduleStatus.SCHEDULED, LocalDate.of(2026, 5, 2));
			ScheduleSnapshot snapshot = snapshotWith(null, null);

			assertThat(ScheduleProgress.advance(meta, snapshot).getStatus())
				.isEqualTo(ScheduleStatus.SCHEDULED);
		}
	}

	@Nested
	@DisplayName("멱등성과 종단 상태")
	class IdempotencyAndTerminal {

		/** 시트 재저장이 두 번째부터 400을 내던 회귀 버그의 재발 방지. */
		@Test
		@DisplayName("같은 스냅샷으로 여러 번 호출해도 상태가 그대로다")
		void isIdempotent() {
			Schedule meta = metaWith(ScheduleStatus.SCHEDULED);
			ScheduleSnapshot snapshot = snapshotWith(null, List.of(sheetWithMeasuredValue()));

			Schedule once = ScheduleProgress.advance(meta, snapshot);
			Schedule twice = ScheduleProgress.advance(once, snapshot);
			Schedule thrice = ScheduleProgress.advance(twice, snapshot);

			assertThat(once.getStatus()).isEqualTo(ScheduleStatus.MEASURING);
			assertThat(twice.getStatus()).isEqualTo(ScheduleStatus.MEASURING);
			assertThat(thrice.getStatus()).isEqualTo(ScheduleStatus.MEASURING);
		}

		@Test
		@DisplayName("이미 분석 중이면 시트 재저장이 측정 중으로 되돌리지 않는다")
		void doesNotMoveBackward() {
			Schedule meta = metaWith(ScheduleStatus.ANALYZING);
			ScheduleSnapshot snapshot = snapshotWith(null, List.of(sheetWithMeasuredValue()));

			assertThat(ScheduleProgress.advance(meta, snapshot).getStatus())
				.isEqualTo(ScheduleStatus.ANALYZING);
		}

		@Test
		@DisplayName("완료된 계획은 어떤 입력이 들어와도 전진하지 않는다")
		void ignoresCompleted() {
			Schedule meta = metaWith(ScheduleStatus.REPORT_COMPLETED, LocalDate.of(2026, 5, 2));
			ScheduleSnapshot snapshot = snapshotWith(LocalTime.of(9, 30), List.of(sheetWithMeasuredValue()));

			assertThat(ScheduleProgress.advance(meta, snapshot).getStatus())
				.isEqualTo(ScheduleStatus.REPORT_COMPLETED);
		}

		@Test
		@DisplayName("취소된 계획은 어떤 입력이 들어와도 전진하지 않는다")
		void ignoresCanceled() {
			Schedule meta = metaWith(ScheduleStatus.CANCELED, LocalDate.of(2026, 5, 2));
			ScheduleSnapshot snapshot = snapshotWith(LocalTime.of(9, 30), List.of(sheetWithMeasuredValue()));

			assertThat(ScheduleProgress.advance(meta, snapshot).getStatus())
				.isEqualTo(ScheduleStatus.CANCELED);
		}
	}
}
