package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.schedule.application.FakeAnalysisRecordRepository;
import com.ensolution.ems.schedule.application.FakeMeasurementRecordRepository;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.domain.analysis.AnalysisRecord;
import com.ensolution.ems.schedule.domain.history.MeasurementRecord;
import com.ensolution.ems.schedule.domain.history.MeasurementResult;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이행 기록·해제 검증.
 * 완료가 이행을 만들고 재개방이 되돌린다는 것, 그리고 몇 번을 호출해도 결과가 같다는 것이 핵심 계약이다.
 */
class MeasurementRecordRecorderTest {

	private static final Long TENANT_ID = 10L;
	private static final Long STACK_ID = 100L;
	private static final Long SCHEDULE_ID = 1L;
	private static final LocalDate SAMPLED_AT = LocalDate.of(2026, 2, 10);
	private static final BigDecimal ALLOWANCE = new BigDecimal("100");

	private FakeMeasurementRecordRepository repository;
	private FakeAnalysisRecordRepository analysisRepository;
	private MeasurementRecordRecorder recorder;

	@BeforeEach
	void setUp() {
		repository = new FakeMeasurementRecordRepository();
		analysisRepository = new FakeAnalysisRecordRepository();
		recorder = new MeasurementRecordRecorder(repository, new AnalysisRecordIndexer(analysisRepository));
	}

	private static Schedule completedSchedule() {
		return Schedule.builder()
			.id(SCHEDULE_ID)
			.tenantId(TENANT_ID)
			.stackId(STACK_ID)
			.teamId(200L)
			.measurementField(MeasurementField.AIR)
			.sampledAt(SAMPLED_AT)
			.status(ScheduleStatus.REPORT_COMPLETED)
			.build();
	}

	private static SamplingItemSnapshot item(Long stackPollutantId, Long pollutantId, MeasurementCycle cycle) {
		return item(stackPollutantId, pollutantId, cycle, true);
	}

	private static SamplingItemSnapshot item(
		Long stackPollutantId, Long pollutantId, MeasurementCycle cycle, boolean oxygenApplicable) {
		return new SamplingItemSnapshot(
			stackPollutantId, pollutantId, "NOX", "질소산화물", "NOx",
			MeasurementField.AIR, null, null, null, null,
			cycle, ALLOWANCE, oxygenApplicable);
	}

	/** 분석 결과 씨앗. 도메인 팩토리를 거쳐 실제 등록 경로와 같은 값이 담기게 한다. */
	private AnalysisRecord analysis(SamplingItemSnapshot item, String value, String unit) {
		return analysisRepository.save(AnalysisRecord.register(
			TENANT_ID, SCHEDULE_ID, item, new BigDecimal(value), unit, "주시험법", "분석장비"));
	}

	private static ScheduleSnapshot snapshotWith(SamplingItemSnapshot... items) {
		return new ScheduleSnapshot(
			String.valueOf(SCHEDULE_ID), SCHEDULE_ID, TENANT_ID, ScheduleStatus.REPORT_COMPLETED,
			null, null, null, null, List.of(), List.of(items), List.of(), 0L, null);
	}

	@Nested
	@DisplayName("완료 — 측정항목 수만큼 이행을 남긴다")
	class RecordCompletion {

		@Test
		@DisplayName("항목마다 한 행씩 측정일의 구간으로 기록한다")
		void recordsEachItem() {
			recorder.recordCompletion(completedSchedule(), snapshotWith(
				item(1L, 11L, MeasurementCycle.QUARTERLY),
				item(2L, 22L, MeasurementCycle.MONTHLY)));

			List<MeasurementRecord> saved = repository.all();
			assertThat(saved).hasSize(2);
			assertThat(saved).allSatisfy(record -> {
				assertThat(record.scheduleId()).isEqualTo(SCHEDULE_ID);
				assertThat(record.stackId()).isEqualTo(STACK_ID);
				assertThat(record.sampledAt()).isEqualTo(SAMPLED_AT);
				assertThat(record.periodYear()).isEqualTo(2026);
			});
			// 2월 10일 → 분기 주기는 1분기, 월 주기는 2월
			assertThat(saved.get(0).period().key()).isEqualTo("2026-Q1");
			assertThat(saved.get(1).period().key()).isEqualTo("2026-M02");
		}

		@Test
		@DisplayName("측정 시점 값(주기·허용기준·물질명)을 행에 복사해 원장 변경에서 격리한다")
		void copiesSnapshotValues() {
			recorder.recordCompletion(completedSchedule(),
				snapshotWith(item(1L, 11L, MeasurementCycle.QUARTERLY)));

			MeasurementRecord record = repository.all().getFirst();
			assertThat(record.cycle()).isEqualTo(MeasurementCycle.QUARTERLY);
			assertThat(record.pollutantCode()).isEqualTo("NOX");
			assertThat(record.pollutantNameKr()).isEqualTo("질소산화물");
		}

		@Test
		@DisplayName("주기가 없는 항목도 이력에는 남지만 이행 집계에는 오르지 않는다")
		void keepsUnscheduledItemOutOfFulfillment() {
			recorder.recordCompletion(completedSchedule(), snapshotWith(item(1L, 11L, null)));

			MeasurementRecord record = repository.all().getFirst();
			assertThat(record.periodIndex()).isNull();
			assertThat(record.period()).isNull();
			assertThat(record.countsTowardFulfillment()).isFalse();
			// 주기가 없어도 연도별 조회에는 걸려야 한다
			assertThat(record.periodYear()).isEqualTo(2026);
		}

		@Test
		@DisplayName("원장 연결키가 없는 옛 스냅샷도 물질만 특정되면 기록한다")
		void recordsWithoutStackPollutantId() {
			recorder.recordCompletion(completedSchedule(),
				snapshotWith(item(null, 11L, MeasurementCycle.ANNUAL)));

			assertThat(repository.all()).hasSize(1);
			assertThat(repository.all().getFirst().stackPollutantId()).isNull();
		}

		@Test
		@DisplayName("어느 물질인지 알 수 없는 항목은 건너뛰고 완료 자체는 막지 않는다")
		void skipsUnidentifiableItem() {
			recorder.recordCompletion(completedSchedule(), snapshotWith(
				item(1L, null, MeasurementCycle.QUARTERLY),
				item(2L, 22L, MeasurementCycle.QUARTERLY)));

			assertThat(repository.all()).hasSize(1);
			assertThat(repository.all().getFirst().pollutantId()).isEqualTo(22L);
		}

		@Test
		@DisplayName("측정항목이 없으면 아무것도 남기지 않는다")
		void doesNothingWithoutItems() {
			recorder.recordCompletion(completedSchedule(), snapshotWith());

			assertThat(repository.all()).isEmpty();
		}

		@Test
		@DisplayName("측정일이 없으면 구간을 정할 수 없으므로 기록하지 않는다")
		void doesNothingWithoutSampledAt() {
			Schedule noDate = completedSchedule().toBuilder().sampledAt(null).build();

			recorder.recordCompletion(noDate, snapshotWith(item(1L, 11L, MeasurementCycle.ANNUAL)));

			assertThat(repository.all()).isEmpty();
		}
	}

	@Nested
	@DisplayName("결과값 — 분석 결과를 이행 기록으로 옮긴다")
	class ResultValues {

		@Test
		@DisplayName("분석값과 단위를 이행 기록에 복사한다")
		void copiesAnalysisValue() {
			SamplingItemSnapshot item = item(1L, 11L, MeasurementCycle.QUARTERLY);
			analysis(item, "80", "ppm");

			recorder.recordCompletion(completedSchedule(), snapshotWith(item));

			MeasurementResult result = repository.all().getFirst().result();
			assertThat(result.concentration()).isEqualByComparingTo("80");
			assertThat(result.unit()).isEqualTo("ppm");
			assertThat(result.allowance()).isEqualByComparingTo(ALLOWANCE);
		}

		@Test
		@DisplayName("산소보정 대상이 아니면 실측값으로 초과를 판정한다")
		void judgesByAnalysisValueWhenOxygenNotApplicable() {
			SamplingItemSnapshot item = item(1L, 11L, MeasurementCycle.QUARTERLY, false);
			analysis(item, "120", "ppm");

			recorder.recordCompletion(completedSchedule(), snapshotWith(item));

			assertThat(repository.all().getFirst().result().exceeded()).isTrue();
		}

		@Test
		@DisplayName("기준 이내면 초과가 아니라고 판정한다")
		void judgesWithinAllowance() {
			SamplingItemSnapshot item = item(1L, 11L, MeasurementCycle.QUARTERLY, false);
			analysis(item, "80", "ppm");

			recorder.recordCompletion(completedSchedule(), snapshotWith(item));

			assertThat(repository.all().getFirst().result().exceeded()).isFalse();
		}

		@Test
		@DisplayName("산소보정 대상은 보정농도가 아직 없으므로 판정을 보류한다")
		void holdsJudgementWhenOxygenApplicable() {
			SamplingItemSnapshot item = item(1L, 11L, MeasurementCycle.QUARTERLY, true);
			analysis(item, "120", "ppm");

			recorder.recordCompletion(completedSchedule(), snapshotWith(item));

			MeasurementResult result = repository.all().getFirst().result();
			// 실측은 기준을 넘지만 비교 대상인 보정 농도가 없다
			assertThat(result.correctedConcentration()).isNull();
			assertThat(result.exceeded()).isNull();
		}

		@Test
		@DisplayName("분석 결과가 없는 항목도 이행은 남기고 허용기준만 채운다")
		void keepsRecordWithoutAnalysis() {
			recorder.recordCompletion(completedSchedule(),
				snapshotWith(item(1L, 11L, MeasurementCycle.QUARTERLY)));

			MeasurementResult result = repository.all().getFirst().result();
			assertThat(result.hasValue()).isFalse();
			assertThat(result.allowance()).isEqualByComparingTo(ALLOWANCE);
			assertThat(result.exceeded()).isNull();
		}

		@Test
		@DisplayName("다른 계획의 분석 결과는 섞이지 않는다")
		void ignoresOtherSchedulesAnalysis() {
			SamplingItemSnapshot item = item(1L, 11L, MeasurementCycle.QUARTERLY);
			analysisRepository.save(AnalysisRecord.register(
				TENANT_ID, 2L, item, new BigDecimal("80"), "ppm", "주시험법", "분석장비"));

			recorder.recordCompletion(completedSchedule(), snapshotWith(item));

			assertThat(repository.all().getFirst().result().hasValue()).isFalse();
		}

		@Test
		@DisplayName("같은 물질의 분석 결과가 둘이면 나중 것을 쓰고 완료를 막지 않는다")
		void prefersLatestOnDuplicatePollutant() {
			SamplingItemSnapshot item = item(1L, 11L, MeasurementCycle.QUARTERLY);
			analysis(item, "80", "ppm");
			analysis(item, "95", "ppm");

			recorder.recordCompletion(completedSchedule(), snapshotWith(item));

			assertThat(repository.all()).hasSize(1);
			assertThat(repository.all().getFirst().result().concentration()).isEqualByComparingTo("95");
		}

		@Test
		@DisplayName("측정물질을 알 수 없는 분석 결과가 있어도 완료를 막지 않는다")
		void toleratesUnidentifiableAnalysis() {
			SamplingItemSnapshot item = item(1L, 11L, MeasurementCycle.QUARTERLY);
			analysisRepository.save(
				AnalysisRecord.register(TENANT_ID, SCHEDULE_ID, item(1L, null, null),
					new BigDecimal("80"), "ppm", "주시험법", "분석장비"));

			recorder.recordCompletion(completedSchedule(), snapshotWith(item));

			assertThat(repository.all()).hasSize(1);
			assertThat(repository.all().getFirst().result().hasValue()).isFalse();
		}
	}

	@Nested
	@DisplayName("해제 — 완료가 풀리면 이행도 풀린다")
	class Revoke {

		@Test
		@DisplayName("그 계획이 만든 이행만 지운다")
		void removesOnlyOwnRecords() {
			recorder.recordCompletion(completedSchedule(),
				snapshotWith(item(1L, 11L, MeasurementCycle.QUARTERLY)));
			Schedule other = completedSchedule().toBuilder().id(2L).build();
			recorder.recordCompletion(other, snapshotWith(item(3L, 33L, MeasurementCycle.QUARTERLY)));

			recorder.revoke(SCHEDULE_ID, TENANT_ID);

			assertThat(repository.all()).hasSize(1);
			assertThat(repository.all().getFirst().scheduleId()).isEqualTo(2L);
		}

		@Test
		@DisplayName("지울 기록이 없어도 실패하지 않는다")
		void toleratesMissingRecords() {
			recorder.revoke(999L, TENANT_ID);

			assertThat(repository.all()).isEmpty();
		}
	}

	@Nested
	@DisplayName("멱등성 — 완료 → 재개방 → 재완료를 견딘다")
	class Idempotency {

		@Test
		@DisplayName("이행 해제 없이 다시 완료해도 기록이 중복되지 않는다")
		void doesNotDuplicateOnRecompletion() {
			ScheduleSnapshot snapshot = snapshotWith(item(1L, 11L, MeasurementCycle.QUARTERLY));

			recorder.recordCompletion(completedSchedule(), snapshot);
			recorder.recordCompletion(completedSchedule(), snapshot);

			assertThat(repository.all()).hasSize(1);
		}

		@Test
		@DisplayName("항목을 줄여 다시 완료하면 빠진 항목의 이행이 남지 않는다")
		void dropsRemovedItems() {
			recorder.recordCompletion(completedSchedule(), snapshotWith(
				item(1L, 11L, MeasurementCycle.QUARTERLY),
				item(2L, 22L, MeasurementCycle.QUARTERLY)));

			recorder.recordCompletion(completedSchedule(),
				snapshotWith(item(1L, 11L, MeasurementCycle.QUARTERLY)));

			assertThat(repository.all()).hasSize(1);
			assertThat(repository.all().getFirst().pollutantId()).isEqualTo(11L);
		}

		@Test
		@DisplayName("재완료해도 결과값이 그대로 다시 채워진다")
		void refillsResultOnRecompletion() {
			SamplingItemSnapshot item = item(1L, 11L, MeasurementCycle.QUARTERLY, false);
			analysis(item, "120", "ppm");
			ScheduleSnapshot snapshot = snapshotWith(item);

			recorder.recordCompletion(completedSchedule(), snapshot);
			recorder.recordCompletion(completedSchedule(), snapshot);

			assertThat(repository.all()).hasSize(1);
			MeasurementResult result = repository.all().getFirst().result();
			assertThat(result.concentration()).isEqualByComparingTo("120");
			assertThat(result.exceeded()).isTrue();
		}
	}
}
