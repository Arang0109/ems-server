package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.FakeAnalysisRecordRepository;
import com.ensolution.ems.schedule.application.FakeScheduleDocumentRepository;
import com.ensolution.ems.schedule.application.FakeScheduleRepository;
import com.ensolution.ems.schedule.application.command.create.CreateAnalysisRecordCommand;
import com.ensolution.ems.schedule.application.command.update.SaveAnalysisResultsCommand;
import com.ensolution.ems.schedule.application.command.update.SaveSamplingTimesCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateAnalysisRecordCommand;
import com.ensolution.ems.schedule.application.validator.AnalysisRecordValidator;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.domain.analysis.AnalysisRecord;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실험분석정보 규칙 검증.
 * 분석값은 <b>이번 계획의 측정항목</b>에만 붙을 수 있고, 판정 근거(허용기준치·산소보정 적용)는
 * 측정 시점 스냅샷에서 복사돼 이후 수정으로 흔들리지 않아야 한다.
 */
class AnalysisRecordServiceTest {

	private static final Long TENANT = 1L;
	private static final Long SCHEDULE = 1L;
	private static final Long NOX = 100L;
	private static final Long SOX = 200L;
	/** 이 계획의 측정항목이 아닌 물질. */
	private static final Long DUST = 300L;

	private FakeScheduleRepository scheduleRepository;
	private FakeScheduleDocumentRepository documentRepository;
	private FakeAnalysisRecordRepository analysisRecordRepository;
	private AnalysisRecordService service;

	@BeforeEach
	void setUp() {
		scheduleRepository = new FakeScheduleRepository();
		documentRepository = new FakeScheduleDocumentRepository();
		analysisRecordRepository = new FakeAnalysisRecordRepository();
		service = new AnalysisRecordService(
			scheduleRepository,
			documentRepository,
			analysisRecordRepository,
			new AnalysisRecordValidator(analysisRecordRepository),
			new AnalysisRecordIndexer(analysisRecordRepository));

		givenSchedule(ScheduleStatus.MEASURING);
	}

	private void givenSchedule(ScheduleStatus status) {
		scheduleRepository.given(Schedule.builder()
			.id(SCHEDULE)
			.tenantId(TENANT)
			.stackId(10L)
			.teamId(20L)
			.measurementField(MeasurementField.AIR)
			.sampledAt(LocalDate.of(2026, 8, 1))
			.status(status)
			.build());

		documentRepository.save(new ScheduleSnapshot(
			String.valueOf(SCHEDULE), SCHEDULE, TENANT, status,
			null, null, null, null, List.of(),
			List.of(item(NOX, new BigDecimal("150"), true), item(SOX, new BigDecimal("80"), false)),
			List.of(), 0L, null));
	}

	private SamplingItemSnapshot item(Long pollutantId, BigDecimal allowance, boolean oxygenApplicable) {
		return new SamplingItemSnapshot(
			pollutantId + 900, pollutantId, "NOX", "질소산화물", "Nitrogen Oxides",
			MeasurementField.AIR, MeasurementMethod.FIELD_MEASUREMENT, PollutantPhase.GAS,
			"분석장비", "공정시험기준", MeasurementCycle.MONTHLY, allowance, oxygenApplicable);
	}

	private CreateAnalysisRecordCommand command(Long pollutantId, String value) {
		return new CreateAnalysisRecordCommand(
			pollutantId, new BigDecimal(value), "ppm", "자외선형광법", "SO2 분석기");
	}

	@Nested
	@DisplayName("등록")
	class Create {

		@Test
		@DisplayName("허용기준치·산소보정 적용 여부는 계획 스냅샷의 측정항목에서 복사된다")
		void copiesLedgerValuesFromSnapshot() {
			AnalysisRecord saved = service.createAnalysis(SCHEDULE, TENANT, command(NOX, "120.5"));

			assertThat(saved.getId()).isNotBlank();
			assertThat(saved.getScheduleId()).isEqualTo(SCHEDULE);
			assertThat(saved.getPollutantId()).isEqualTo(NOX);
			assertThat(saved.getStackPollutantId()).isEqualTo(NOX + 900);
			assertThat(saved.getPollutantName()).isEqualTo("질소산화물");
			assertThat(saved.getAllowance()).isEqualByComparingTo("150");
			assertThat(saved.isOxygenApplicable()).isTrue();
			assertThat(saved.getAnalysisValue()).isEqualByComparingTo("120.5");
			assertThat(saved.getUnit()).isEqualTo("ppm");
		}

		@Test
		@DisplayName("이번 계획의 측정항목이 아니면 거부한다")
		void rejectsPollutantOutsideSchedule() {
			assertThatThrownBy(() -> service.createAnalysis(SCHEDULE, TENANT, command(DUST, "10")))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_ITEM_NOT_IN_SCHEDULE);
		}

		@Test
		@DisplayName("같은 측정항목을 두 번 등록할 수 없다")
		void rejectsDuplicatePollutant() {
			service.createAnalysis(SCHEDULE, TENANT, command(NOX, "120"));

			assertThatThrownBy(() -> service.createAnalysis(SCHEDULE, TENANT, command(NOX, "130")))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_ANALYSIS_ALREADY_EXISTS);
		}

		@Test
		@DisplayName("완료된 계획에는 등록할 수 없다")
		void rejectsCompletedSchedule() {
			givenSchedule(ScheduleStatus.REPORT_COMPLETED);

			assertThatThrownBy(() -> service.createAnalysis(SCHEDULE, TENANT, command(NOX, "120")))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_EDITABLE);
		}
	}

	@Nested
	@DisplayName("수정·삭제")
	class Modify {

		@Test
		@DisplayName("전달하지 않은 필드는 유지되고 허용기준치는 바뀌지 않는다")
		void keepsUntouchedFields() {
			AnalysisRecord saved = service.createAnalysis(SCHEDULE, TENANT, command(NOX, "120"));

			AnalysisRecord updated = service.updateAnalysis(SCHEDULE, TENANT, saved.getId(),
				new UpdateAnalysisRecordCommand(new BigDecimal("99.9"), null, null, "  "));

			assertThat(updated.getAnalysisValue()).isEqualByComparingTo("99.9");
			assertThat(updated.getUnit()).isEqualTo("ppm");
			assertThat(updated.getAnalysisMethod()).isEqualTo("자외선형광법");
			assertThat(updated.getAnalysisEquipment()).isEqualTo("SO2 분석기");
			assertThat(updated.getAllowance()).isEqualByComparingTo("150");
			assertThat(analysisRecordRepository.all()).hasSize(1);
		}

		@Test
		@DisplayName("다른 측정계획의 기록은 건드릴 수 없다")
		void rejectsRecordOfAnotherSchedule() {
			AnalysisRecord saved = service.createAnalysis(SCHEDULE, TENANT, command(NOX, "120"));
			Long otherSchedule = SCHEDULE + 1;
			scheduleRepository.given(Schedule.builder()
				.id(otherSchedule).tenantId(TENANT).status(ScheduleStatus.MEASURING).build());

			assertThatThrownBy(() -> service.deleteAnalysis(otherSchedule, TENANT, saved.getId()))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_ANALYSIS_NOT_FOUND);
		}

		@Test
		@DisplayName("삭제하면 같은 항목을 다시 등록할 수 있다")
		void allowsReRegistrationAfterDelete() {
			AnalysisRecord saved = service.createAnalysis(SCHEDULE, TENANT, command(NOX, "120"));

			service.deleteAnalysis(SCHEDULE, TENANT, saved.getId());

			assertThat(analysisRecordRepository.all()).isEmpty();
			assertThat(service.createAnalysis(SCHEDULE, TENANT, command(NOX, "130")).getAnalysisValue())
				.isEqualByComparingTo("130");
		}
	}

	@Nested
	@DisplayName("채취시간 일괄 저장 — 성적서 탭")
	class SaveSamplingTimes {

		private SaveSamplingTimesCommand.Entry entry(Long pollutantId, LocalTime start, LocalTime end) {
			return new SaveSamplingTimesCommand.Entry(pollutantId, start, end);
		}

		private SaveSamplingTimesCommand cmd(SaveSamplingTimesCommand.Entry... entries) {
			return new SaveSamplingTimesCommand(List.of(entries));
		}

		@Test
		@DisplayName("기록이 없는 항목은 채취시간만 가진 기록을 새로 만든다")
		void createsRecordWhenAbsent() {
			List<AnalysisRecord> saved = service.saveSamplingTimes(SCHEDULE, TENANT,
				cmd(entry(NOX, LocalTime.of(9, 30), LocalTime.of(10, 0))));

			assertThat(saved).hasSize(1);
			AnalysisRecord record = saved.getFirst();
			assertThat(record.getId()).isNotBlank();
			assertThat(record.getPollutantId()).isEqualTo(NOX);
			assertThat(record.getSamplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
			assertThat(record.getSamplingEndedAt()).isEqualTo(LocalTime.of(10, 0));
			assertThat(record.getAnalysisValue()).isNull();
			// 판정 근거는 등록 경로와 똑같이 스냅샷에서 복사한다
			assertThat(record.getAllowance()).isEqualByComparingTo("150");
			assertThat(record.isOxygenApplicable()).isTrue();
		}

		@Test
		@DisplayName("기존 기록이 있으면 채취시간만 갱신하고 분석값은 보존한다 — 필드 소유 분리의 핵심 계약")
		void keepsLabValuesOfExistingRecord() {
			service.createAnalysis(SCHEDULE, TENANT, command(NOX, "120.5"));

			AnalysisRecord updated = service.saveSamplingTimes(SCHEDULE, TENANT,
				cmd(entry(NOX, LocalTime.of(9, 0), LocalTime.of(9, 30)))).getFirst();

			assertThat(updated.getSamplingStartedAt()).isEqualTo(LocalTime.of(9, 0));
			assertThat(updated.getAnalysisValue()).isEqualByComparingTo("120.5");
			assertThat(updated.getUnit()).isEqualTo("ppm");
			assertThat(updated.getAnalysisMethod()).isEqualTo("자외선형광법");
			// 새 문서를 만들지 않고 기존 문서를 갱신해야 한다
			assertThat(analysisRecordRepository.all()).hasSize(1);
		}

		@Test
		@DisplayName("전달한 항목의 빈 시각은 기존 값을 지운다 — 표의 빈 칸은 '지웠다'는 뜻이다")
		void clearsTimesWhenBlankSent() {
			service.saveSamplingTimes(SCHEDULE, TENANT,
				cmd(entry(NOX, LocalTime.of(9, 30), LocalTime.of(10, 0))));

			AnalysisRecord cleared = service.saveSamplingTimes(SCHEDULE, TENANT,
				cmd(entry(NOX, null, null))).getFirst();

			assertThat(cleared.getSamplingStartedAt()).isNull();
			assertThat(cleared.getSamplingEndedAt()).isNull();
		}

		@Test
		@DisplayName("한 번도 손대지 않은 빈 행은 문서를 만들지 않는다")
		void skipsUntouchedRows() {
			List<AnalysisRecord> saved = service.saveSamplingTimes(SCHEDULE, TENANT,
				cmd(entry(NOX, LocalTime.of(9, 30), LocalTime.of(10, 0)), entry(SOX, null, null)));

			assertThat(saved).hasSize(1);
			assertThat(analysisRecordRepository.all()).hasSize(1);
		}

		@Test
		@DisplayName("여러 항목을 한 번에 저장한다")
		void savesMultipleItems() {
			List<AnalysisRecord> saved = service.saveSamplingTimes(SCHEDULE, TENANT,
				cmd(entry(NOX, LocalTime.of(9, 0), LocalTime.of(9, 30)),
					entry(SOX, LocalTime.of(10, 0), LocalTime.of(10, 30))));

			assertThat(saved).extracting(AnalysisRecord::getPollutantId).containsExactly(NOX, SOX);
			assertThat(analysisRecordRepository.all()).hasSize(2);
		}

		@Test
		@DisplayName("이번 계획의 측정항목이 아니면 거부한다")
		void rejectsPollutantOutsideSchedule() {
			assertThatThrownBy(() -> service.saveSamplingTimes(SCHEDULE, TENANT,
				cmd(entry(DUST, LocalTime.of(9, 0), null))))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_ITEM_NOT_IN_SCHEDULE);
		}

		@Test
		@DisplayName("같은 측정항목이 두 번 담기면 거부한다 — 어느 시각이 맞는지 정할 근거가 없다")
		void rejectsDuplicatePollutantInRequest() {
			assertThatThrownBy(() -> service.saveSamplingTimes(SCHEDULE, TENANT,
				cmd(entry(NOX, LocalTime.of(9, 0), null), entry(NOX, LocalTime.of(11, 0), null))))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_ANALYSIS_DUPLICATE_ITEM);
		}

		@Test
		@DisplayName("중복이 있으면 아무것도 저장하지 않는다 — 검증이 쓰기보다 먼저다")
		void savesNothingWhenRequestRejected() {
			assertThatThrownBy(() -> service.saveSamplingTimes(SCHEDULE, TENANT,
				cmd(entry(NOX, LocalTime.of(9, 0), null), entry(NOX, LocalTime.of(11, 0), null))))
				.isInstanceOf(CustomException.class);

			assertThat(analysisRecordRepository.all()).isEmpty();
		}

		@Test
		@DisplayName("완료된 계획에는 저장할 수 없다")
		void rejectsCompletedSchedule() {
			givenSchedule(ScheduleStatus.REPORT_COMPLETED);

			assertThatThrownBy(() -> service.saveSamplingTimes(SCHEDULE, TENANT,
				cmd(entry(NOX, LocalTime.of(9, 0), null))))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_EDITABLE);
		}

		@Test
		@DisplayName("빈 목록이어도 깨지지 않는다")
		void toleratesEmptyRequest() {
			assertThat(service.saveSamplingTimes(SCHEDULE, TENANT, new SaveSamplingTimesCommand(List.of())))
				.isEmpty();
		}
	}

	@Nested
	@DisplayName("분석 결과 일괄 저장 — 실험·분석 탭")
	class SaveAnalysisResults {

		private SaveAnalysisResultsCommand.Entry entry(Long pollutantId, String value) {
			return new SaveAnalysisResultsCommand.Entry(
				pollutantId, value == null ? null : new BigDecimal(value),
				"ppm", "자외선형광법", "NOx 분석기");
		}

		private SaveAnalysisResultsCommand cmd(SaveAnalysisResultsCommand.Entry... entries) {
			return new SaveAnalysisResultsCommand(List.of(entries));
		}

		@Test
		@DisplayName("성적서 탭이 먼저 문서를 만들어 두어도 충돌하지 않는다 — 측정물질을 키로 upsert 한다")
		void upsertsOntoRecordCreatedByReportTab() {
			// 성적서 탭에서 채취시간을 먼저 저장해 문서가 생긴 상태
			service.saveSamplingTimes(SCHEDULE, TENANT,
				new SaveSamplingTimesCommand(List.of(
					new SaveSamplingTimesCommand.Entry(NOX, LocalTime.of(9, 30), LocalTime.of(10, 0)))));

			AnalysisRecord saved = service.saveAnalysisResults(SCHEDULE, TENANT, cmd(entry(NOX, "120.5")))
				.getFirst();

			assertThat(saved.getAnalysisValue()).isEqualByComparingTo("120.5");
			// 채취시간은 그대로 남아야 한다 — 실험·분석 탭이 소유하지 않는 필드다
			assertThat(saved.getSamplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
			assertThat(saved.getSamplingEndedAt()).isEqualTo(LocalTime.of(10, 0));
			// 문서를 새로 만들지 않고 기존 문서를 갱신해야 한다
			assertThat(analysisRecordRepository.all()).hasSize(1);
		}

		@Test
		@DisplayName("반대 순서도 성립한다 — 분석값을 먼저 넣어도 채취시간 저장이 덮어쓰지 않는다")
		void samplingTimesDoNotClobberResults() {
			service.saveAnalysisResults(SCHEDULE, TENANT, cmd(entry(NOX, "120.5")));

			AnalysisRecord saved = service.saveSamplingTimes(SCHEDULE, TENANT,
				new SaveSamplingTimesCommand(List.of(
					new SaveSamplingTimesCommand.Entry(NOX, LocalTime.of(9, 30), null)))).getFirst();

			assertThat(saved.getSamplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
			assertThat(saved.getAnalysisValue()).isEqualByComparingTo("120.5");
			assertThat(analysisRecordRepository.all()).hasSize(1);
		}

		@Test
		@DisplayName("기록이 없는 항목은 새로 만든다")
		void createsRecordWhenAbsent() {
			AnalysisRecord saved = service.saveAnalysisResults(SCHEDULE, TENANT, cmd(entry(NOX, "120.5")))
				.getFirst();

			assertThat(saved.getId()).isNotBlank();
			assertThat(saved.getAnalysisValue()).isEqualByComparingTo("120.5");
			// 판정 근거는 스냅샷에서 복사한다
			assertThat(saved.getAllowance()).isEqualByComparingTo("150");
			assertThat(saved.isOxygenApplicable()).isTrue();
		}

		@Test
		@DisplayName("전달한 항목의 빈 값은 기존 값을 지운다 — 표의 빈 칸은 '지웠다'는 뜻이다")
		void clearsValuesWhenBlankSent() {
			service.saveAnalysisResults(SCHEDULE, TENANT, cmd(entry(NOX, "120.5")));

			AnalysisRecord cleared = service.saveAnalysisResults(SCHEDULE, TENANT,
				cmd(new SaveAnalysisResultsCommand.Entry(NOX, null, null, null, null))).getFirst();

			assertThat(cleared.getAnalysisValue()).isNull();
			assertThat(cleared.getUnit()).isNull();
		}

		@Test
		@DisplayName("한 번도 손대지 않은 빈 행은 문서를 만들지 않는다")
		void skipsUntouchedRows() {
			List<AnalysisRecord> saved = service.saveAnalysisResults(SCHEDULE, TENANT,
				cmd(entry(NOX, "120.5"),
					new SaveAnalysisResultsCommand.Entry(SOX, null, null, null, null)));

			assertThat(saved).hasSize(1);
			assertThat(analysisRecordRepository.all()).hasSize(1);
		}

		@Test
		@DisplayName("이번 계획의 측정항목이 아니면 거부한다")
		void rejectsPollutantOutsideSchedule() {
			assertThatThrownBy(() -> service.saveAnalysisResults(SCHEDULE, TENANT, cmd(entry(DUST, "10"))))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_ITEM_NOT_IN_SCHEDULE);
		}

		@Test
		@DisplayName("같은 측정항목이 두 번 담기면 아무것도 저장하지 않고 거부한다")
		void rejectsDuplicatePollutantInRequest() {
			assertThatThrownBy(() -> service.saveAnalysisResults(SCHEDULE, TENANT,
				cmd(entry(NOX, "120"), entry(NOX, "130"))))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_ANALYSIS_DUPLICATE_ITEM);

			assertThat(analysisRecordRepository.all()).isEmpty();
		}

		@Test
		@DisplayName("완료된 계획에는 저장할 수 없다")
		void rejectsCompletedSchedule() {
			givenSchedule(ScheduleStatus.REPORT_COMPLETED);

			assertThatThrownBy(() -> service.saveAnalysisResults(SCHEDULE, TENANT, cmd(entry(NOX, "120"))))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_EDITABLE);
		}

		@Test
		@DisplayName("여러 번 저장해도 결과가 같다 — 표를 다시 눌러도 문서가 늘지 않는다")
		void isIdempotent() {
			service.saveAnalysisResults(SCHEDULE, TENANT, cmd(entry(NOX, "120.5")));
			service.saveAnalysisResults(SCHEDULE, TENANT, cmd(entry(NOX, "120.5")));

			assertThat(analysisRecordRepository.all()).hasSize(1);
		}
	}
}
