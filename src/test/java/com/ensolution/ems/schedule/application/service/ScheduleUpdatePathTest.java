package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.service.support.ScheduleStatusTransitioner;
import com.ensolution.ems.schedule.application.service.support.SnapshotWriter;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.FakeScheduleDocumentRepository;
import com.ensolution.ems.schedule.application.FakeScheduleRepository;
import com.ensolution.ems.schedule.application.command.detail.ScheduleDetail;
import com.ensolution.ems.schedule.application.command.update.UpdateBasicInfoCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateScheduleCommand;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.domain.snapshot.SamplingSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.TeamSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.TenantSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 측정계획 수정 경로 둘의 경계와 null 시맨틱 검증.
 *
 * <p>두 경로는 <b>서로 다른 값을 소유</b>한다 — {@code PUT /{id}}는 계획을 정의하는 값
 * (채취일자·측정용도·관리번호), {@code PATCH /{id}/basic-info}는 진행하며 채우는 값
 * (접수·분석완료·발행일과 채취시각·담당자)이다. 한쪽이 다른 쪽 값을 건드리면 사용자가 다른 탭에서
 * 입력한 것이 조용히 사라지므로, 그 경계를 여기서 고정한다.
 *
 * <p>null의 뜻도 갈린다. {@code PUT}은 한 화면이 단독으로 소유하므로 <b>전체 채택</b>(빈 값 = 지움),
 * {@code PATCH}는 두 화면이 공유하며 서로의 칸에 null을 실어 보내므로 <b>부분 갱신</b>(null = 유지)이다.
 *
 * <p>이 클래스가 없던 동안 두 경로가 서로의 값을 지우는 결함이 남아 있었다. 회귀 방어선이다.
 */
class ScheduleUpdatePathTest {

	private static final Long TENANT = 1L;
	private static final Long OTHER_TENANT = 99L;
	private static final Long SCHEDULE = 1L;

	private static final LocalDate SAMPLED_AT = LocalDate.of(2026, 5, 1);
	private static final LocalDate RECEIVED_AT = LocalDate.of(2026, 5, 2);
	private static final LocalDate ANALYZED_AT = LocalDate.of(2026, 5, 4);
	private static final LocalDate ISSUED_AT = LocalDate.of(2026, 5, 8);

	private FakeScheduleRepository scheduleRepository;
	private FakeScheduleDocumentRepository documentRepository;

	/** {@code PUT /{id}} — 계획을 정의하는 값. */
	private ScheduleService scheduleService;
	/** {@code PATCH /{id}/basic-info} — 진행하며 채우는 값. 두 경로가 서로 다른 서비스에 산다. */
	private ScheduleSnapshotService snapshotService;

	@BeforeEach
	void setUp() {
		scheduleRepository = new FakeScheduleRepository();
		documentRepository = new FakeScheduleDocumentRepository();

		// 조립·재계산·이력·탐색·목록 협력자는 이 두 경로가 지나지 않는다.
		scheduleService = new ScheduleService(
			scheduleRepository, documentRepository,
			null, null, null, null,
			new ScheduleStatusTransitioner(scheduleRepository, documentRepository, null));

		snapshotService = new ScheduleSnapshotService(
			scheduleRepository, null, null,
			new SnapshotWriter(documentRepository), null,
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
			new TeamSnapshot(20L, "1팀", "홍길동", "김철수", List.of()),
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

	/** 실험·분석 탭이 보내는 모양 — 일자와 서명란 담당자만 채우고 나머지는 null이다. */
	private static UpdateBasicInfoCommand analysisTabOf(
		LocalDate receivedAt, LocalDate analyzedAt, LocalDate issuedAt, String analyst, String technicalManager
	) {
		return new UpdateBasicInfoCommand(
			null, null, analyst, technicalManager,
			receivedAt, analyzedAt, issuedAt,
			null, null, null, null);
	}

	/** 현장 채취 탭이 보내는 모양 — 채취시각·현장 담당자·측정자만 채우고 일자는 null이다. */
	private static UpdateBasicInfoCommand samplingTabOf(
		LocalTime startedAt, LocalTime endedAt, String facilityManager, String samplingWitness
	) {
		return new UpdateBasicInfoCommand(
			facilityManager, samplingWitness, null, null,
			null, null, null,
			startedAt, endedAt, null, null);
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
	@DisplayName("PATCH /{id}/basic-info — 진행하며 채우는 값")
	class UpdateBasicInfo {

		/** 이 경로는 관리번호를 소유하지 않는다. 건드리면 성적서의 관리번호가 사라진다. */
		@Test
		void 관리번호와_측정용도를_건드리지_않는다() {
			snapshotService.updateBasicInfo(SCHEDULE, TENANT,
				analysisTabOf(LocalDate.of(2026, 5, 3), null, null, null, null));

			assertThat(savedMeta().getReferenceNumber()).isEqualTo("2026-A-001");
			assertThat(savedMeta().getSchedulePurpose()).isEqualTo("자가측정용");
			assertThat(savedMeta().getSampledAt()).isEqualTo(SAMPLED_AT);
		}

		@Test
		void 전달한_일자만_갱신한다() {
			snapshotService.updateBasicInfo(SCHEDULE, TENANT,
				analysisTabOf(null, null, LocalDate.of(2026, 5, 9), null, null));

			assertThat(savedMeta().getIssuedAt()).isEqualTo(LocalDate.of(2026, 5, 9));
			// 나머지 둘은 미전달이므로 유지된다 — 지움이 아니다.
			assertThat(savedMeta().getReceivedAt()).isEqualTo(RECEIVED_AT);
			assertThat(savedMeta().getAnalyzedAt()).isEqualTo(ANALYZED_AT);
		}

		/** 현장 채취 탭은 일자 셋에 null을 실어 보낸다. */
		@Test
		void 현장_채취_탭_저장이_실험분석_탭의_일자를_지우지_않는다() {
			snapshotService.updateBasicInfo(SCHEDULE, TENANT,
				samplingTabOf(LocalTime.of(10, 0), LocalTime.of(12, 0), "박관리", null));

			assertThat(savedMeta().getReceivedAt()).isEqualTo(RECEIVED_AT);
			assertThat(savedMeta().getAnalyzedAt()).isEqualTo(ANALYZED_AT);
			assertThat(savedMeta().getIssuedAt()).isEqualTo(ISSUED_AT);
			assertThat(savedSnapshot().samplingData().samplingStartedAt()).isEqualTo(LocalTime.of(10, 0));
			assertThat(savedSnapshot().samplingData().facilityManager()).isEqualTo("박관리");
		}

		/** 실험·분석 탭은 채취시각·현장 담당자·측정자에 null을 실어 보낸다. */
		@Test
		void 실험분석_탭_저장이_현장_채취_탭의_입력을_지우지_않는다() {
			snapshotService.updateBasicInfo(SCHEDULE, TENANT,
				analysisTabOf(LocalDate.of(2026, 5, 3), null, null, "이분석", null));

			assertThat(savedSnapshot().samplingData().samplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
			assertThat(savedSnapshot().samplingData().samplingEndedAt()).isEqualTo(LocalTime.of(11, 0));
			assertThat(savedSnapshot().samplingData().facilityManager()).isEqualTo("이관리");
			assertThat(savedSnapshot().samplingData().samplingWitness()).isEqualTo("정입회");
			assertThat(savedSnapshot().team().mentorName()).isEqualTo("홍길동");
			assertThat(savedSnapshot().tenant().analyst()).isEqualTo("이분석");
			assertThat(savedSnapshot().tenant().technicalManager()).isEqualTo("최기술");
		}

		@Test
		void 시료접수일이_채워지면_분석값_입력_중으로_전진한다() {
			// 접수일이 없는 상태에서 시작해야 전이가 눈에 보인다.
			scheduleRepository.given(savedMeta().toBuilder().receivedAt(null).build());

			ScheduleDetail detail = snapshotService.updateBasicInfo(SCHEDULE, TENANT,
				analysisTabOf(LocalDate.of(2026, 5, 3), null, null, null, null));

			assertThat(detail.meta().getStatus()).isEqualTo(ScheduleStatus.ANALYZING);
		}

		@Test
		void 완료된_계획은_수정할_수_없다() {
			givenSchedule(ScheduleStatus.REPORT_COMPLETED);

			assertThatThrownBy(() -> snapshotService.updateBasicInfo(SCHEDULE, TENANT,
				analysisTabOf(null, null, LocalDate.of(2026, 5, 9), null, null)))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_EDITABLE);
		}

		@Test
		void 다른_고객사의_계획은_찾지_못한다() {
			assertThatThrownBy(() -> snapshotService.updateBasicInfo(SCHEDULE, OTHER_TENANT,
				analysisTabOf(null, null, LocalDate.of(2026, 5, 9), null, null)))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_NOT_FOUND);
		}
	}
}
