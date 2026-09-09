package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.FakeScheduleDocumentRepository;
import com.ensolution.ems.schedule.application.FakeScheduleRepository;
import com.ensolution.ems.schedule.application.command.detail.ScheduleDetail;
import com.ensolution.ems.schedule.application.command.update.ChangeTeamSnapshotCommand;
import com.ensolution.ems.schedule.application.command.update.ChangeTenantSnapshotCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateReportDatesCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateScheduleCommand;
import com.ensolution.ems.schedule.application.event.EditorRef;
import com.ensolution.ems.schedule.application.event.SheetsSavedEvent;
import com.ensolution.ems.schedule.application.port.out.ScheduleEventBroadcaster;
import com.ensolution.ems.schedule.application.service.support.ScheduleStatusTransitioner;
import com.ensolution.ems.schedule.application.service.support.SnapshotSheetReCalculator;
import com.ensolution.ems.schedule.application.service.support.SnapshotWriter;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.domain.sampling.MeasurementCategory;
import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;
import com.ensolution.ems.schedule.domain.snapshot.EquipmentSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.SamplingSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.TeamSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.TenantSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 측정계획 수정 경로 다섯의 경계와 null 시맨틱 검증.
 *
 * <p>한때 성적서를 진행하며 채우는 값 열한 개가 {@code PATCH /{id}/basic-info} 한 경로로 저장됐다.
 * 두 화면이 그 경로를 공유해 서로 자기 것이 아닌 칸에 null을 실어 보냈고, 그래서 부분 갱신일 수밖에
 * 없어 <b>이미 채운 값을 비울 방법이 없었다.</b> 화면별로 경로를 쪼개면서 각 경로가 단독 소유가 되었고,
 * 일자 셋은 전체 채택으로 바뀌어 비우기가 가능해졌다.
 *
 * <p>이 클래스가 고정하는 것은 <b>경로가 서로의 값을 건드리지 않는다</b>는 것이다. 한쪽이 다른 쪽 값을
 * 지우면 사용자가 다른 탭에서 입력한 것이 조용히 사라진다 — 분할 이전에 실제로 있던 결함이다.
 *
 * <table>
 *   <caption>경로별 소유와 null 시맨틱</caption>
 *   <tr><th>경로</th><th>소유</th><th>null</th></tr>
 *   <tr><td>{@code PUT /{id}}</td><td>채취일자·측정용도·관리번호</td><td>전체 채택</td></tr>
 *   <tr><td>{@code PATCH /{id}/report-dates}</td><td>접수·분석완료·발행일</td><td>전체 채택</td></tr>
 *   <tr><td>{@code PATCH /{id}/tenant}</td><td>서명란 담당자</td><td>부분 갱신(두 탭 공유)</td></tr>
 *   <tr><td>{@code PATCH /{id}/team}</td><td>측정자 표기</td><td>부분 갱신</td></tr>
 *   <tr><td>{@code PUT /{id}/sheets}</td><td>채취시각·현장 담당자</td><td>부분 갱신</td></tr>
 * </table>
 */
class ScheduleUpdatePathTest {

	private static final Long TENANT = 1L;
	private static final Long OTHER_TENANT = 99L;
	private static final Long SCHEDULE = 1L;

	private static final LocalDate SAMPLED_AT = LocalDate.of(2026, 5, 1);
	private static final LocalDate RECEIVED_AT = LocalDate.of(2026, 5, 2);
	private static final LocalDate ANALYZED_AT = LocalDate.of(2026, 5, 4);
	private static final LocalDate ISSUED_AT = LocalDate.of(2026, 5, 8);

	/** 시트 저장은 알림을 발행하지만 이 경로는 SSE 구독까지 가지 않는다. */
	private static final ScheduleEventBroadcaster NOOP_BROADCASTER = new ScheduleEventBroadcaster() {
		@Override
		public void subscribe(Long scheduleId, Long tenantId, SseEmitter emitter) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void publishSheetsSaved(SheetsSavedEvent event) {
		}
	};

	private FakeScheduleRepository scheduleRepository;
	private FakeScheduleDocumentRepository documentRepository;

	/** {@code PUT /{id}} 와 {@code PATCH /{id}/report-dates} — 메타를 쓰는 두 경로. */
	private ScheduleService scheduleService;
	/** {@code PATCH /{id}/tenant} · {@code /team} — 문서를 쓰는 두 경로. */
	private ScheduleSnapshotService snapshotService;
	/** {@code PUT /{id}/sheets} — 채취시각·현장 담당자가 시트와 함께 저장된다. */
	private ScheduleSheetService sheetService;

	@BeforeEach
	void setUp() {
		scheduleRepository = new FakeScheduleRepository();
		documentRepository = new FakeScheduleDocumentRepository();

		// 조립·이력·탐색·목록 협력자는 이 경로들이 지나지 않는다.
		scheduleService = new ScheduleService(
			scheduleRepository, documentRepository,
			null, null, null, null,
			new ScheduleStatusTransitioner(scheduleRepository, documentRepository, null));

		snapshotService = new ScheduleSnapshotService(
			scheduleRepository, null, null,
			new SnapshotWriter(documentRepository), null,
			new ScheduleStatusTransitioner(scheduleRepository, documentRepository, null));

		// 측정시설 스냅샷이 없어 계산 입력이 없으므로 재계산은 시트를 그대로 돌려준다.
		sheetService = new ScheduleSheetService(
			scheduleRepository,
			new SnapshotWriter(documentRepository),
			new SnapshotSheetReCalculator(null),
			null,
			NOOP_BROADCASTER,
			new ScheduleStatusTransitioner(scheduleRepository, documentRepository, null));

		givenSchedule(ScheduleStatus.MEASURING);
		givenSnapshot();
	}

	/** 관리번호·측정용도와 성적서 진행 일자가 모두 채워진 계획. */
	private void givenSchedule(ScheduleStatus status) {
		scheduleRepository.given(Schedule.builder()
			.id(SCHEDULE)
			.tenantId(TENANT)
			.stackId(10L)
			.teamId(20L)
			.measurementField(MeasurementField.AIR)
			.schedulePurpose("자가측정용")
			.referenceNumber("2026-A-001")
			.sampledAt(SAMPLED_AT)
			.receivedAt(RECEIVED_AT)
			.analyzedAt(ANALYZED_AT)
			.issuedAt(ISSUED_AT)
			.status(status)
			.build());
	}

	private void givenSnapshot() {
		documentRepository.given(new ScheduleSnapshot(
			String.valueOf(SCHEDULE), SCHEDULE, TENANT, 0L,
			null,
			new TenantSnapshot(TENANT, "고객사", null, null, null, null, null, "박분석", "최기술"),
			new TeamSnapshot(20L, "1팀", "홍길동", "김철수",
				List.of(new EquipmentSnapshot("E1", EquipType.PITOT_TUBE,
					null, null, null, null, null, null, null, null))),
			new SamplingSnapshot(LocalTime.of(9, 30), LocalTime.of(11, 0), "이관리", "정입회", List.of()),
			List.of()));
	}

	private Schedule savedMeta() {
		return scheduleRepository.findById(SCHEDULE, TENANT);
	}

	private ScheduleSnapshot savedSnapshot() {
		return documentRepository.findByScheduleId(SCHEDULE, TENANT);
	}

	/** 측정정보 탭이 실제로 보내는 모양 — 이 셋만 담는다. */
	private static UpdateScheduleCommand planOf(LocalDate sampledAt, String purpose, String referenceNumber) {
		return new UpdateScheduleCommand(sampledAt, purpose, referenceNumber);
	}

	/** 실험·분석 탭이 보내는 일자 폼 — 자기 필드 전부를 보낸다. */
	private static UpdateReportDatesCommand datesOf(LocalDate receivedAt, LocalDate analyzedAt, LocalDate issuedAt) {
		return new UpdateReportDatesCommand(receivedAt, analyzedAt, issuedAt);
	}

	private static ChangeTenantSnapshotCommand staffOf(String analyst, String technicalManager) {
		return new ChangeTenantSnapshotCommand(null, null, null, null, null, null, analyst, technicalManager);
	}

	@Nested
	@DisplayName("PUT /{id} — 계획을 정의하는 값")
	class UpdatePlan {

		/** 측정정보 탭은 이 세 일자를 아예 보내지 않는다. 건드리면 실험·분석 탭 입력이 사라진다. */
		@Test
		void 성적서_진행_일자를_건드리지_않는다() {
			scheduleService.updateMeta(SCHEDULE, TENANT, planOf(SAMPLED_AT, "기타참고용", "2026-A-002"));

			assertThat(savedMeta().getReceivedAt()).isEqualTo(RECEIVED_AT);
			assertThat(savedMeta().getAnalyzedAt()).isEqualTo(ANALYZED_AT);
			assertThat(savedMeta().getIssuedAt()).isEqualTo(ISSUED_AT);
		}

		@Test
		void 측정용도와_관리번호를_교체한다() {
			scheduleService.updateMeta(SCHEDULE, TENANT, planOf(SAMPLED_AT, "기타참고용", "2026-A-002"));

			assertThat(savedMeta().getSchedulePurpose()).isEqualTo("기타참고용");
			assertThat(savedMeta().getReferenceNumber()).isEqualTo("2026-A-002");
		}

		@Test
		void 빈_측정용도와_관리번호는_지운다() {
			scheduleService.updateMeta(SCHEDULE, TENANT, planOf(SAMPLED_AT, null, null));

			assertThat(savedMeta().getSchedulePurpose()).isNull();
			assertThat(savedMeta().getReferenceNumber()).isNull();
		}

		@Test
		void 채취일자는_비울_수_없다() {
			scheduleService.updateMeta(SCHEDULE, TENANT, planOf(null, "자가측정용", "2026-A-001"));

			assertThat(savedMeta().getSampledAt()).isEqualTo(SAMPLED_AT);
		}

		@Test
		void 측정분야는_바꾸지_않는다() {
			scheduleService.updateMeta(SCHEDULE, TENANT, planOf(SAMPLED_AT, null, null));

			assertThat(savedMeta().getMeasurementField()).isEqualTo(MeasurementField.AIR);
		}

		@Test
		void 문서_스냅샷은_건드리지_않는다() {
			scheduleService.updateMeta(SCHEDULE, TENANT, planOf(SAMPLED_AT, "기타참고용", "2026-A-002"));

			assertThat(savedSnapshot().samplingData().facilityManager()).isEqualTo("이관리");
			assertThat(savedSnapshot().tenant().analyst()).isEqualTo("박분석");
			assertThat(savedSnapshot().team().mentorName()).isEqualTo("홍길동");
		}

		@Test
		void 완료된_계획은_수정할_수_없다() {
			givenSchedule(ScheduleStatus.REPORT_COMPLETED);

			assertThatThrownBy(() -> scheduleService.updateMeta(SCHEDULE, TENANT, planOf(SAMPLED_AT, null, null)))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_EDITABLE);
		}

		@Test
		void 다른_고객사의_계획은_찾지_못한다() {
			assertThatThrownBy(() -> scheduleService.updateMeta(SCHEDULE, OTHER_TENANT, planOf(SAMPLED_AT, null, null)))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("PATCH /{id}/report-dates — 성적서를 진행하며 채우는 일자")
	class UpdateReportDates {

		/** 이 경로는 관리번호를 소유하지 않는다. 건드리면 성적서의 관리번호가 사라진다. */
		@Test
		void 관리번호와_측정용도와_채취일자를_건드리지_않는다() {
			scheduleService.updateReportDates(SCHEDULE, TENANT,
				datesOf(LocalDate.of(2026, 5, 3), ANALYZED_AT, ISSUED_AT));

			assertThat(savedMeta().getReferenceNumber()).isEqualTo("2026-A-001");
			assertThat(savedMeta().getSchedulePurpose()).isEqualTo("자가측정용");
			assertThat(savedMeta().getSampledAt()).isEqualTo(SAMPLED_AT);
		}

		@Test
		void 전달한_일자를_그대로_채택한다() {
			scheduleService.updateReportDates(SCHEDULE, TENANT,
				datesOf(LocalDate.of(2026, 5, 3), LocalDate.of(2026, 5, 5), LocalDate.of(2026, 5, 9)));

			assertThat(savedMeta().getReceivedAt()).isEqualTo(LocalDate.of(2026, 5, 3));
			assertThat(savedMeta().getAnalyzedAt()).isEqualTo(LocalDate.of(2026, 5, 5));
			assertThat(savedMeta().getIssuedAt()).isEqualTo(LocalDate.of(2026, 5, 9));
		}

		/** 분할 이전에는 불가능했던 동작이다 — 잘못 넣은 일자를 되돌릴 방법이 없었다. */
		@Test
		void 빈_일자는_지운다() {
			scheduleService.updateReportDates(SCHEDULE, TENANT, datesOf(RECEIVED_AT, ANALYZED_AT, null));

			assertThat(savedMeta().getIssuedAt()).isNull();
			assertThat(savedMeta().getReceivedAt()).isEqualTo(RECEIVED_AT);
			assertThat(savedMeta().getAnalyzedAt()).isEqualTo(ANALYZED_AT);
		}

		@Test
		void 문서_스냅샷은_건드리지_않는다() {
			scheduleService.updateReportDates(SCHEDULE, TENANT, datesOf(RECEIVED_AT, ANALYZED_AT, ISSUED_AT));

			assertThat(savedSnapshot().samplingData().samplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
			assertThat(savedSnapshot().tenant().analyst()).isEqualTo("박분석");
			assertThat(savedSnapshot().version()).isZero();
		}

		@Test
		void 발행일이_채취일보다_빠르면_거부한다() {
			assertThatThrownBy(() -> scheduleService.updateReportDates(SCHEDULE, TENANT,
				datesOf(null, null, SAMPLED_AT.minusDays(1))))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_INVALID_CHRONOLOGY);
		}

		@Test
		void 분석완료일이_접수일보다_빠르면_거부한다() {
			assertThatThrownBy(() -> scheduleService.updateReportDates(SCHEDULE, TENANT,
				datesOf(LocalDate.of(2026, 5, 5), LocalDate.of(2026, 5, 3), null)))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_INVALID_CHRONOLOGY);
		}

		@Test
		void 시료접수일이_채워지면_분석값_입력_중으로_전진한다() {
			// 접수일이 없는 상태에서 시작해야 전이가 눈에 보인다.
			scheduleRepository.given(savedMeta().toBuilder()
				.receivedAt(null).analyzedAt(null).issuedAt(null).build());

			ScheduleDetail detail = scheduleService.updateReportDates(SCHEDULE, TENANT,
				datesOf(LocalDate.of(2026, 5, 3), null, null));

			assertThat(detail.meta().getStatus()).isEqualTo(ScheduleStatus.ANALYZING);
		}

		@Test
		void 완료된_계획은_수정할_수_없다() {
			givenSchedule(ScheduleStatus.REPORT_COMPLETED);

			assertThatThrownBy(() -> scheduleService.updateReportDates(SCHEDULE, TENANT,
				datesOf(RECEIVED_AT, ANALYZED_AT, ISSUED_AT)))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_EDITABLE);
		}

		@Test
		void 다른_고객사의_계획은_찾지_못한다() {
			assertThatThrownBy(() -> scheduleService.updateReportDates(SCHEDULE, OTHER_TENANT,
				datesOf(RECEIVED_AT, ANALYZED_AT, ISSUED_AT)))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("PATCH /{id}/tenant — 성적서 서명란 담당자")
	class ChangeTenant {

		@Test
		void 서명란_담당자를_교체한다() {
			snapshotService.changeTenant(SCHEDULE, TENANT, staffOf("이분석", "정기술"));

			assertThat(savedSnapshot().tenant().analyst()).isEqualTo("이분석");
			assertThat(savedSnapshot().tenant().technicalManager()).isEqualTo("정기술");
		}

		/** 두 탭이 공유하는 경로다. 자기 것이 아닌 칸의 빈 값이 상대의 입력을 지우면 안 된다. */
		@Test
		void 전달되지_않은_담당자는_기존_값을_유지한다() {
			snapshotService.changeTenant(SCHEDULE, TENANT, staffOf("이분석", null));

			assertThat(savedSnapshot().tenant().analyst()).isEqualTo("이분석");
			assertThat(savedSnapshot().tenant().technicalManager()).isEqualTo("최기술");
		}

		@Test
		void 원장_연결키와_고객사명은_보존된다() {
			snapshotService.changeTenant(SCHEDULE, TENANT, staffOf("이분석", null));

			assertThat(savedSnapshot().tenant().tenantId()).isEqualTo(TENANT);
			assertThat(savedSnapshot().tenant().name()).isEqualTo("고객사");
		}

		@Test
		void 메타와_다른_스냅샷_노드는_건드리지_않는다() {
			snapshotService.changeTenant(SCHEDULE, TENANT, staffOf("이분석", "정기술"));

			assertThat(savedMeta().getReferenceNumber()).isEqualTo("2026-A-001");
			assertThat(savedMeta().getIssuedAt()).isEqualTo(ISSUED_AT);
			assertThat(savedSnapshot().team().mentorName()).isEqualTo("홍길동");
			assertThat(savedSnapshot().samplingData().facilityManager()).isEqualTo("이관리");
		}

		@Test
		void 완료된_계획은_수정할_수_없다() {
			givenSchedule(ScheduleStatus.REPORT_COMPLETED);

			assertThatThrownBy(() -> snapshotService.changeTenant(SCHEDULE, TENANT, staffOf("이분석", null)))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_EDITABLE);
		}

		@Test
		void 다른_고객사의_계획은_찾지_못한다() {
			assertThatThrownBy(() -> snapshotService.changeTenant(SCHEDULE, OTHER_TENANT, staffOf("이분석", null)))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("PATCH /{id}/team — 측정자 표기")
	class ChangeTeam {

		@Test
		void 측정자_표기명을_교체한다() {
			snapshotService.changeTeam(SCHEDULE, TENANT, new ChangeTeamSnapshotCommand("이측정", "박보조"));

			assertThat(savedSnapshot().team().mentorName()).isEqualTo("이측정");
			assertThat(savedSnapshot().team().menteeName()).isEqualTo("박보조");
		}

		@Test
		void 전달되지_않은_이름은_기존_값을_유지한다() {
			snapshotService.changeTeam(SCHEDULE, TENANT, new ChangeTeamSnapshotCommand(null, "박보조"));

			assertThat(savedSnapshot().team().mentorName()).isEqualTo("홍길동");
			assertThat(savedSnapshot().team().menteeName()).isEqualTo("박보조");
		}

		/** 이 경로는 장비를 소유하지 않는다. 비어 온 장비 목록을 채택하면 이 회차 장비가 통째로 사라진다. */
		@Test
		void 원장_연결키와_장비_목록은_보존된다() {
			snapshotService.changeTeam(SCHEDULE, TENANT, new ChangeTeamSnapshotCommand("이측정", "박보조"));

			assertThat(savedSnapshot().team().teamId()).isEqualTo(20L);
			assertThat(savedSnapshot().team().teamName()).isEqualTo("1팀");
			assertThat(savedSnapshot().team().equipments())
				.extracting(EquipmentSnapshot::equipmentId).containsExactly("E1");
		}

		@Test
		void 메타와_다른_스냅샷_노드는_건드리지_않는다() {
			snapshotService.changeTeam(SCHEDULE, TENANT, new ChangeTeamSnapshotCommand("이측정", "박보조"));

			assertThat(savedMeta().getReferenceNumber()).isEqualTo("2026-A-001");
			assertThat(savedSnapshot().tenant().analyst()).isEqualTo("박분석");
			assertThat(savedSnapshot().samplingData().samplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
		}

		@Test
		void 다른_고객사의_계획은_찾지_못한다() {
			assertThatThrownBy(() -> snapshotService.changeTeam(SCHEDULE, OTHER_TENANT,
				new ChangeTeamSnapshotCommand("이측정", null)))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("PUT /{id}/sheets — 채취시각·현장 담당자는 시트와 함께 저장된다")
	class SaveSamplingInfo {

		private final EditorRef editor = new EditorRef("tester", "테스터");

		private ScheduleDetail save(LocalTime startedAt, LocalTime endedAt,
		                            String facilityManager, String samplingWitness) {
			return sheetService.saveSheets(SCHEDULE, TENANT, editor,
				startedAt, endedAt, facilityManager, samplingWitness,
				List.of(SamplingSheet.builder().category(MeasurementCategory.GAS).version(0L).build()),
				List.of());
		}

		/** 한때 이 값들이 저장 경로 밖에서 버려져 화면에 남지 않았다. */
		@Test
		void 채취시각과_현장_담당자가_실제로_저장된다() {
			save(LocalTime.of(10, 0), LocalTime.of(12, 0), "박관리", "한입회");

			assertThat(savedSnapshot().samplingData().samplingStartedAt()).isEqualTo(LocalTime.of(10, 0));
			assertThat(savedSnapshot().samplingData().samplingEndedAt()).isEqualTo(LocalTime.of(12, 0));
			assertThat(savedSnapshot().samplingData().facilityManager()).isEqualTo("박관리");
			assertThat(savedSnapshot().samplingData().samplingWitness()).isEqualTo("한입회");
		}

		@Test
		void 전달되지_않은_값은_기존_값을_유지한다() {
			save(null, null, null, null);

			assertThat(savedSnapshot().samplingData().samplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
			assertThat(savedSnapshot().samplingData().samplingEndedAt()).isEqualTo(LocalTime.of(11, 0));
			assertThat(savedSnapshot().samplingData().facilityManager()).isEqualTo("이관리");
			assertThat(savedSnapshot().samplingData().samplingWitness()).isEqualTo("정입회");
		}

		@Test
		void 시트도_함께_저장된다() {
			save(LocalTime.of(10, 0), null, null, null);

			assertThat(savedSnapshot().sheets()).extracting(SamplingSheet::getCategory)
				.containsExactly(MeasurementCategory.GAS);
		}

		@Test
		void 메타와_다른_스냅샷_노드는_건드리지_않는다() {
			save(LocalTime.of(10, 0), null, "박관리", null);

			assertThat(savedMeta().getReferenceNumber()).isEqualTo("2026-A-001");
			assertThat(savedMeta().getIssuedAt()).isEqualTo(ISSUED_AT);
			assertThat(savedSnapshot().tenant().analyst()).isEqualTo("박분석");
			assertThat(savedSnapshot().team().mentorName()).isEqualTo("홍길동");
		}

		/** 채취 스냅샷이 아직 없는 문서도 이 경로를 탈 수 있어야 한다. */
		@Test
		void 채취_스냅샷이_없는_문서에도_저장된다() {
			documentRepository.given(new ScheduleSnapshot(
				String.valueOf(SCHEDULE), SCHEDULE, TENANT, 0L,
				null, null, null, null, List.of()));

			save(LocalTime.of(10, 0), null, "박관리", null);

			assertThat(savedSnapshot().samplingData().samplingStartedAt()).isEqualTo(LocalTime.of(10, 0));
			assertThat(savedSnapshot().samplingData().facilityManager()).isEqualTo("박관리");
		}

		@Test
		void 분석값_입력_중이면_거부한다() {
			givenSchedule(ScheduleStatus.ANALYZING);

			assertThatThrownBy(() -> save(LocalTime.of(10, 0), null, null, null))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_SHEET_NOT_EDITABLE);
		}

		@Test
		void 다른_고객사의_계획은_찾지_못한다() {
			assertThatThrownBy(() -> sheetService.saveSheets(SCHEDULE, OTHER_TENANT, editor,
				LocalTime.of(10, 0), null, null, null, List.of(), List.of()))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_FOUND);
		}
	}
}
