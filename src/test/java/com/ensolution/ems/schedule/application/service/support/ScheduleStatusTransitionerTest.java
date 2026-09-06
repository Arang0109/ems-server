package com.ensolution.ems.schedule.application.service.support;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.schedule.application.FakeMeasurementRecordRepository;
import com.ensolution.ems.schedule.application.FakeScheduleDocumentRepository;
import com.ensolution.ems.schedule.application.FakeScheduleRepository;
import com.ensolution.ems.schedule.application.command.detail.ScheduleDetail;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.domain.sampling.MeasurementCategory;
import com.ensolution.ems.schedule.domain.sampling.SamplingPoint;
import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.SamplingSnapshot;
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
 * 상태 전이의 <b>문서 저장 시점</b>을 고정한다.
 *
 * <p>이 클래스가 존재하는 이유가 그것이다 — 전이 경로가 둘인데 어느 쪽이 MongoDB 문서를 쓰는지가
 * 다르고, 예전에는 이름이 비슷한 private 헬퍼 셋으로 흩어져 있었다. 상태는 메타(MySQL)에만 있으므로
 * 문서에 되비출 것이 없는데, 여기서 문서를 한 번 더 쓰면 저장할 이유가 없는 쓰기가 낙관적 락을
 * 건드려 다른 사용자의 저장을 밀어낸다.
 *
 * <p>문서 저장 여부는 {@link FakeScheduleDocumentRepository}가 저장 성공 시 version을 1 올리는 것으로
 * 판정한다 — version이 그대로면 쓰지 않았다는 뜻이다.
 *
 * <p>완료 훅(이행 이력 기록·해제)이 사용자 확정 경로 한 곳에만 붙는다는 것도 함께 고정한다.
 * 자동 전이가 이력을 건드리면 성적서 작성이 끝나지 않은 회차의 이행이 현황판에 남는다.
 */
class ScheduleStatusTransitionerTest {

	private static final Long TENANT = 1L;
	private static final Long SCHEDULE = 10L;
	private static final Long STACK = 100L;
	private static final LocalDate SAMPLED_AT = LocalDate.of(2026, 5, 1);

	private FakeScheduleRepository scheduleRepository;
	private FakeScheduleDocumentRepository documentRepository;
	private FakeMeasurementRecordRepository recordRepository;
	private ScheduleStatusTransitioner transitioner;

	@BeforeEach
	void setUp() {
		scheduleRepository = new FakeScheduleRepository();
		documentRepository = new FakeScheduleDocumentRepository();
		recordRepository = new FakeMeasurementRecordRepository();

		transitioner = new ScheduleStatusTransitioner(
			scheduleRepository, documentRepository, new MeasurementRecordRecorder(recordRepository));
	}

	private Schedule givenSchedule(ScheduleStatus status, LocalDate receivedAt) {
		return scheduleRepository.given(Schedule.builder()
			.id(SCHEDULE)
			.tenantId(TENANT)
			.stackId(STACK)
			.teamId(20L)
			.measurementField(MeasurementField.AIR)
			.sampledAt(SAMPLED_AT)
			.receivedAt(receivedAt)
			.status(status)
			.build());
	}

	/** 측정항목 하나짜리 문서. 시트가 없으면 실측값도 없다. */
	private ScheduleSnapshot givenSnapshot(SamplingSnapshot sampling) {
		ScheduleSnapshot snapshot = new ScheduleSnapshot(
			String.valueOf(SCHEDULE), SCHEDULE, TENANT, 0L,
			null, null, null, sampling, List.of(item()));
		documentRepository.given(snapshot);
		return snapshot;
	}

	private static SamplingItemSnapshot item() {
		return new SamplingItemSnapshot(
			1L, 11L, "NOX", "질소산화물", "NOx",
			MeasurementField.AIR, null, null, null, null,
			MeasurementCycle.QUARTERLY, new BigDecimal("100"), true, null);
	}

	/** 실측값(배출가스 온도)이 하나 들어온 기록지. 측정 착수 판정의 근거다. */
	private static SamplingSnapshot samplingWithMeasuredValue() {
		return new SamplingSnapshot(null, null, null, null, List.of(
			SamplingSheet.builder()
				.category(MeasurementCategory.DUST)
				.samplingPoints(List.of(
					SamplingPoint.builder().gasTemperature(new BigDecimal("100")).build()))
				.build()));
	}

	private Long storedVersion() {
		return documentRepository.findByScheduleId(SCHEDULE, TENANT).version();
	}

	@Nested
	@DisplayName("confirmTransition — 사용자 확정 전이")
	class ConfirmTransition {

		@Test
		void 메타만_저장하고_문서는_쓰지_않는다() {
			Schedule meta = givenSchedule(ScheduleStatus.ANALYZING, null);
			givenSnapshot(null);

			ScheduleDetail detail = transitioner.confirmTransition(meta, meta.complete());

			assertThat(detail.meta().getStatus()).isEqualTo(ScheduleStatus.REPORT_COMPLETED);
			assertThat(scheduleRepository.findById(SCHEDULE, TENANT).getStatus())
				.isEqualTo(ScheduleStatus.REPORT_COMPLETED);
			assertThat(storedVersion()).isEqualTo(0L);
		}

		@Test
		void 완료로_확정되면_항목별_이행을_남긴다() {
			Schedule meta = givenSchedule(ScheduleStatus.ANALYZING, null);
			givenSnapshot(null);

			transitioner.confirmTransition(meta, meta.complete());

			assertThat(recordRepository.all()).hasSize(1);
		}

		@Test
		void 취소는_이행을_남기지_않는다() {
			Schedule meta = givenSchedule(ScheduleStatus.MEASURING, null);
			givenSnapshot(null);

			transitioner.confirmTransition(meta, meta.cancel());

			assertThat(recordRepository.all()).isEmpty();
			assertThat(storedVersion()).isEqualTo(0L);
		}

		@Test
		void 완료가_풀리면_그_계획이_만든_이행을_되돌린다() {
			Schedule completed = givenSchedule(ScheduleStatus.ANALYZING, null);
			ScheduleSnapshot snapshot = givenSnapshot(null);
			transitioner.confirmTransition(completed, completed.complete());
			assertThat(recordRepository.all()).hasSize(1);

			Schedule meta = scheduleRepository.findById(SCHEDULE, TENANT);
			transitioner.confirmTransition(meta, meta.reopen(), snapshot);

			assertThat(recordRepository.all()).isEmpty();
			assertThat(storedVersion()).isEqualTo(0L);
		}
	}

	@Nested
	@DisplayName("advanceAfterDocumentSaved — 문서는 이미 저장된 뒤다")
	class AdvanceAfterDocumentSaved {

		@Test
		void 실측값이_들어오면_측정_중으로_전진시키되_문서는_다시_쓰지_않는다() {
			Schedule meta = givenSchedule(ScheduleStatus.SCHEDULED, null);
			ScheduleSnapshot saved = givenSnapshot(samplingWithMeasuredValue());

			ScheduleDetail detail = transitioner.advanceAfterDocumentSaved(meta, saved);

			assertThat(detail.meta().getStatus()).isEqualTo(ScheduleStatus.MEASURING);
			assertThat(scheduleRepository.findById(SCHEDULE, TENANT).getStatus())
				.isEqualTo(ScheduleStatus.MEASURING);
			assertThat(storedVersion()).isEqualTo(0L);
		}

		@Test
		void 상태가_그대로면_메타도_건드리지_않는다() {
			Schedule meta = givenSchedule(ScheduleStatus.SCHEDULED, null);
			ScheduleSnapshot saved = givenSnapshot(null);

			ScheduleDetail detail = transitioner.advanceAfterDocumentSaved(meta, saved);

			// 저장을 거치지 않았다는 것을 넘겨준 인스턴스가 그대로 돌아오는 것으로 고정한다.
			assertThat(detail.meta()).isSameAs(meta);
			assertThat(detail.snapshot()).isSameAs(saved);
			assertThat(storedVersion()).isEqualTo(0L);
		}

		@Test
		void 자동_전이는_성적서_작성_완료_경계를_넘지_않는다() {
			// 시료접수일까지 채워진 상태 — 자동 전이의 상한이다.
			Schedule meta = givenSchedule(ScheduleStatus.MEASURING, LocalDate.of(2026, 5, 2));
			ScheduleSnapshot saved = givenSnapshot(samplingWithMeasuredValue());

			ScheduleDetail detail = transitioner.advanceAfterDocumentSaved(meta, saved);

			assertThat(detail.meta().getStatus()).isEqualTo(ScheduleStatus.ANALYZING);
			// 완료 훅은 사용자 확정 경로에만 있다.
			assertThat(recordRepository.all()).isEmpty();
		}

		@Test
		void 종단_상태는_자동_전이하지_않는다() {
			Schedule meta = givenSchedule(ScheduleStatus.REPORT_COMPLETED, LocalDate.of(2026, 5, 2));
			ScheduleSnapshot saved = givenSnapshot(samplingWithMeasuredValue());

			ScheduleDetail detail = transitioner.advanceAfterDocumentSaved(meta, saved);

			assertThat(detail.meta()).isSameAs(meta);
			assertThat(detail.meta().getStatus()).isEqualTo(ScheduleStatus.REPORT_COMPLETED);
		}
	}
}
