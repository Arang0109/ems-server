package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.service.support.SnapshotWriter;
import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.FakeScheduleDocumentRepository;
import com.ensolution.ems.schedule.application.FakeScheduleRepository;
import com.ensolution.ems.schedule.application.command.update.SaveAnalysisResultsCommand;
import com.ensolution.ems.schedule.application.command.update.SaveSamplingTimesCommand;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.domain.snapshot.AnalysisResult;
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
 *
 * <p>분석값은 <b>이번 계획의 측정항목</b>에만 붙을 수 있고, 판정 근거(허용기준치·산소보정 적용)는
 * 측정 시점 스냅샷 값이라 실험실 입력으로 흔들리지 않아야 한다.
 *
 * <p>분석 결과가 측정 시트와 한 문서에 저장되면서 <b>실험·분석 탭과 성적서 탭이 같은 문서 버전을
 * 놓고 경합</b>하게 됐다. 두 탭이 서로의 입력을 덮어쓰지 않는다는 것이 이 클래스가 고정하는 가장
 * 중요한 계약이다 — 깨지면 실험실 입력이 조용히 사라진다.
 */
class AnalysisResultServiceTest {

	private static final Long TENANT = 1L;
	private static final Long SCHEDULE = 1L;
	private static final Long NOX = 100L;
	private static final Long SOX = 200L;
	/** 이 계획의 측정항목이 아닌 물질. */
	private static final Long DUST = 300L;

	private FakeScheduleRepository scheduleRepository;
	private FakeScheduleDocumentRepository documentRepository;
	private AnalysisResultService service;

	@BeforeEach
	void setUp() {
		scheduleRepository = new FakeScheduleRepository();
		documentRepository = new FakeScheduleDocumentRepository();
		service = new AnalysisResultService(
			scheduleRepository, documentRepository, new SnapshotWriter(documentRepository));

		givenSchedule(ScheduleStatus.MEASURING);
		givenSnapshot(item(NOX, "질소산화물", new BigDecimal("150")), item(SOX, "황산화물", new BigDecimal("70")));
	}

	private void givenSchedule(ScheduleStatus status) {
		scheduleRepository.given(Schedule.builder()
			.id(SCHEDULE)
			.tenantId(TENANT)
			.stackId(10L)
			.teamId(20L)
			.measurementField(MeasurementField.AIR)
			.sampledAt(LocalDate.of(2026, 5, 1))
			.status(status)
			.build());
	}

	private void givenSnapshot(SamplingItemSnapshot... items) {
		documentRepository.given(new ScheduleSnapshot(
			String.valueOf(SCHEDULE), SCHEDULE, TENANT, 0L, null, null, null, null, List.of(items)));
	}

	private static SamplingItemSnapshot item(Long pollutantId, String nameKr, BigDecimal allowance) {
		return new SamplingItemSnapshot(
			pollutantId * 10, pollutantId, null, nameKr, null,
			MeasurementField.AIR, MeasurementMethod.FIELD_MEASUREMENT, PollutantPhase.GAS,
			null, null, MeasurementCycle.MONTHLY, allowance, false, null);
	}

	private AnalysisResult analysisOf(Long pollutantId) {
		return documentRepository.findByScheduleId(SCHEDULE, TENANT).requireItem(pollutantId).analysis();
	}

	private static SaveAnalysisResultsCommand results(Long pollutantId, String value, String unit) {
		return new SaveAnalysisResultsCommand(List.of(new SaveAnalysisResultsCommand.Entry(
			pollutantId, value == null ? null : new BigDecimal(value), unit, "주시험법", "분석기")));
	}

	private static SaveSamplingTimesCommand samplingTimes(Long pollutantId, LocalTime start, LocalTime end) {
		return new SaveSamplingTimesCommand(
			List.of(new SaveSamplingTimesCommand.Entry(pollutantId, start, end)));
	}

	@Nested
	@DisplayName("saveAnalysisResults — 실험·분석 탭")
	class SaveAnalysisResults {

		@Test
		void 결과가_없던_항목을_채운다() {
			service.saveAnalysisResults(SCHEDULE, TENANT, results(NOX, "12.5", "ppm"));

			assertThat(analysisOf(NOX).analysisValue()).isEqualByComparingTo("12.5");
			assertThat(analysisOf(NOX).unit()).isEqualTo("ppm");
		}

		@Test
		void 요청에_없는_항목은_손대지_않는다() {
			service.saveAnalysisResults(SCHEDULE, TENANT, results(NOX, "12.5", "ppm"));

			assertThat(analysisOf(SOX)).isNull();
		}

		@Test
		void 한_번도_손대지_않은_빈_행은_결과를_만들지_않는다() {
			// 네 칸이 모두 비어 있어야 "손대지 않은 행"이다 — 분석방법만 적혀 있어도 저장 대상이다.
			service.saveAnalysisResults(SCHEDULE, TENANT, new SaveAnalysisResultsCommand(
				List.of(new SaveAnalysisResultsCommand.Entry(NOX, null, null, null, null))));

			assertThat(analysisOf(NOX)).isNull();
		}

		@Test
		void 한_칸이라도_채워졌으면_결과를_만든다() {
			service.saveAnalysisResults(SCHEDULE, TENANT, new SaveAnalysisResultsCommand(
				List.of(new SaveAnalysisResultsCommand.Entry(NOX, null, null, "주시험법", null))));

			assertThat(analysisOf(NOX)).isNotNull();
			assertThat(analysisOf(NOX).analysisMethod()).isEqualTo("주시험법");
		}

		@Test
		void 이미_값이_있던_항목의_빈_칸은_지운다() {
			service.saveAnalysisResults(SCHEDULE, TENANT, results(NOX, "12.5", "ppm"));

			service.saveAnalysisResults(SCHEDULE, TENANT,
				new SaveAnalysisResultsCommand(List.of(
					new SaveAnalysisResultsCommand.Entry(NOX, null, null, null, null))));

			assertThat(analysisOf(NOX).analysisValue()).isNull();
			assertThat(analysisOf(NOX).unit()).isNull();
		}

		@Test
		void 판정_근거는_실험실_입력으로_바뀌지_않는다() {
			service.saveAnalysisResults(SCHEDULE, TENANT, results(NOX, "999", "ppm"));

			SamplingItemSnapshot saved =
				documentRepository.findByScheduleId(SCHEDULE, TENANT).requireItem(NOX);
			assertThat(saved.allowance()).isEqualByComparingTo("150");
			assertThat(saved.oxygenApplicable()).isFalse();
		}

		@Test
		void 이번_계획의_측정항목이_아니면_거부한다() {
			assertThatThrownBy(() -> service.saveAnalysisResults(SCHEDULE, TENANT, results(DUST, "1", "ppm")))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_ITEM_NOT_IN_SCHEDULE);
		}

		@Test
		void 요청에_같은_항목이_두_번_담기면_거부한다() {
			SaveAnalysisResultsCommand duplicated = new SaveAnalysisResultsCommand(List.of(
				new SaveAnalysisResultsCommand.Entry(NOX, BigDecimal.ONE, "ppm", null, null),
				new SaveAnalysisResultsCommand.Entry(NOX, BigDecimal.TEN, "ppm", null, null)));

			assertThatThrownBy(() -> service.saveAnalysisResults(SCHEDULE, TENANT, duplicated))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_ANALYSIS_DUPLICATE_ITEM);
		}

		@Test
		void 완료된_계획은_저장할_수_없다() {
			givenSchedule(ScheduleStatus.REPORT_COMPLETED);

			assertThatThrownBy(() -> service.saveAnalysisResults(SCHEDULE, TENANT, results(NOX, "1", "ppm")))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_EDITABLE);
		}

		@Test
		void 다른_고객사의_계획은_찾지_못한다() {
			assertThatThrownBy(() -> service.saveAnalysisResults(SCHEDULE, 999L, results(NOX, "1", "ppm")))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("saveSamplingTimes — 성적서 탭")
	class SaveSamplingTimes {

		@Test
		void 채취시각이_없던_항목을_채운다() {
			service.saveSamplingTimes(SCHEDULE, TENANT,
				samplingTimes(NOX, LocalTime.of(9, 30), LocalTime.of(11, 0)));

			assertThat(analysisOf(NOX).samplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
			assertThat(analysisOf(NOX).samplingEndedAt()).isEqualTo(LocalTime.of(11, 0));
		}

		@Test
		void 한_번도_손대지_않은_빈_행은_결과를_만들지_않는다() {
			service.saveSamplingTimes(SCHEDULE, TENANT, samplingTimes(NOX, null, null));

			assertThat(analysisOf(NOX)).isNull();
		}

		@Test
		void 이미_시각이_있던_항목의_빈_칸은_지운다() {
			service.saveSamplingTimes(SCHEDULE, TENANT,
				samplingTimes(NOX, LocalTime.of(9, 30), LocalTime.of(11, 0)));

			service.saveSamplingTimes(SCHEDULE, TENANT, samplingTimes(NOX, null, null));

			assertThat(analysisOf(NOX).samplingStartedAt()).isNull();
			assertThat(analysisOf(NOX).samplingEndedAt()).isNull();
		}
	}

	@Nested
	@DisplayName("두 탭 동시 저장 — 서로의 입력을 덮어쓰지 않는다")
	class ConcurrentTabs {

		@Test
		void 실험분석_탭_저장이_성적서_탭의_채취시각을_지우지_않는다() {
			service.saveSamplingTimes(SCHEDULE, TENANT,
				samplingTimes(NOX, LocalTime.of(9, 30), LocalTime.of(11, 0)));

			service.saveAnalysisResults(SCHEDULE, TENANT, results(NOX, "12.5", "ppm"));

			AnalysisResult saved = analysisOf(NOX);
			assertThat(saved.analysisValue()).isEqualByComparingTo("12.5");
			assertThat(saved.samplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
			assertThat(saved.samplingEndedAt()).isEqualTo(LocalTime.of(11, 0));
		}

		@Test
		void 성적서_탭_저장이_실험실_입력값을_지우지_않는다() {
			service.saveAnalysisResults(SCHEDULE, TENANT, results(NOX, "12.5", "ppm"));

			service.saveSamplingTimes(SCHEDULE, TENANT,
				samplingTimes(NOX, LocalTime.of(9, 30), LocalTime.of(11, 0)));

			AnalysisResult saved = analysisOf(NOX);
			assertThat(saved.samplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
			assertThat(saved.analysisValue()).isEqualByComparingTo("12.5");
			assertThat(saved.unit()).isEqualTo("ppm");
		}

		/**
		 * 두 저장이 물리적으로 겹쳐도 재시도가 상대 탭의 입력을 되돌리지 않아야 한다.
		 * 변경 함수가 <b>다시 읽은</b> 문서를 기준으로 자기 필드만 적용하는 것이 그 근거다.
		 */
		@Test
		void 저장이_물리적으로_겹치면_다시_읽어_재적용한다() {
			service.saveSamplingTimes(SCHEDULE, TENANT,
				samplingTimes(NOX, LocalTime.of(9, 30), LocalTime.of(11, 0)));
			documentRepository.failNextSaves(1);

			service.saveAnalysisResults(SCHEDULE, TENANT, results(NOX, "12.5", "ppm"));

			AnalysisResult saved = analysisOf(NOX);
			assertThat(saved.analysisValue()).isEqualByComparingTo("12.5");
			assertThat(saved.samplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
		}

		@Test
		void 재시도_한도를_넘으면_충돌을_알린다() {
			documentRepository.failNextSaves(SnapshotWriter.MAX_ATTEMPTS);

			assertThatThrownBy(() -> service.saveAnalysisResults(SCHEDULE, TENANT, results(NOX, "1", "ppm")))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_SHEET_VERSION_CONFLICT);
		}
	}

	@Nested
	@DisplayName("getAnalyses")
	class GetAnalyses {

		@Test
		void 측정항목을_성적서_순서대로_돌려준다() {
			List<SamplingItemSnapshot> items = service.getAnalyses(SCHEDULE, TENANT);

			assertThat(items).extracting(SamplingItemSnapshot::pollutantId).containsExactly(NOX, SOX);
		}

		@Test
		void 아직_분석_전인_항목도_함께_돌려준다() {
			service.saveAnalysisResults(SCHEDULE, TENANT, results(NOX, "12.5", "ppm"));

			List<SamplingItemSnapshot> items = service.getAnalyses(SCHEDULE, TENANT);

			assertThat(items).hasSize(2);
			assertThat(items.getFirst().analysis()).isNotNull();
			assertThat(items.get(1).analysis()).isNull();
		}

		@Test
		void 다른_고객사의_계획은_찾지_못한다() {
			assertThatThrownBy(() -> service.getAnalyses(SCHEDULE, 999L))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_FOUND);
		}
	}
}
