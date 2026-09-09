package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.command.create.CreateScheduleCommand;
import com.ensolution.ems.schedule.application.command.detail.ScheduleDetail;
import com.ensolution.ems.schedule.application.command.list_item.ScheduleListItem;
import com.ensolution.ems.schedule.application.command.update.UpdateReportDatesCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateScheduleCommand;
import com.ensolution.ems.schedule.application.mapper.ScheduleListItemMapper;
import com.ensolution.ems.schedule.application.port.out.ScheduleDocumentRepository;
import com.ensolution.ems.schedule.application.port.out.ScheduleRepository;
import com.ensolution.ems.schedule.application.service.assembler.ScheduleSnapshotAssembler;
import com.ensolution.ems.schedule.application.service.support.MeasurementRecordRecorder;
import com.ensolution.ems.schedule.application.service.support.ScheduleStatusTransitioner;
import com.ensolution.ems.schedule.application.validator.ScheduleValidator;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.ScheduleProgress;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 측정계획 애그리거트의 생명주기 유스케이스 — 생성·삭제·메타 수정·상태 전이·조회.
 *
 * <p>메타(MySQL)를 진실의 원천으로 두고, 세부 스냅샷(MongoDB) 저장/삭제를 각 트랜잭션의 마지막
 * 부수효과로 배치해 정합성을 확보한다(2PC 불가). <b>저장은 MySQL → Mongo, 삭제는 그 역순</b>이다.
 *
 * <p>측정계획 유스케이스는 관심사별로 넷으로 나뉜다. 자기 자리를 찾을 때 아래를 본다.
 * <ul>
 *   <li>{@link ScheduleSnapshotService} — 문서(스냅샷) 편집 6경로</li>
 *   <li>{@link ScheduleSheetService} — 측정 시트 저장·이전 회차 불러오기</li>
 *   <li>{@link ScheduleStatisticsService} — 타 모듈에 여는 측정 건수 통계</li>
 * </ul>
 * 저장 시점이 갈리는 상태 전이는 {@link ScheduleStatusTransitioner}가 넷 모두를 대신 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {

	private final ScheduleRepository scheduleRepository;
	private final ScheduleDocumentRepository scheduleDocumentRepository;
	private final ScheduleSnapshotAssembler snapshotAssembler;
	private final ScheduleValidator scheduleValidator;
	private final ScheduleListItemMapper scheduleListItemMapper;
	private final MeasurementRecordRecorder measurementRecordRecorder;
	private final ScheduleStatusTransitioner statusTransitioner;

	/**
	 * 측정계획을 등록한다.
	 * <p>
	 * 사수·부사수는 <b>이 회차에 나가는 사람</b>이라 메타에 id로 남기고, 성적서에 인쇄될 이름은
	 * 스냅샷이 갖는다({@code ScheduleSnapshotAssembler}가 auth에서 조회해 채운다).
	 * 미지정이면 팀 원장의 사수·부사수 이름이 표기의 기본값이 된다.
	 */
	public ScheduleDetail createSchedule(CreateScheduleCommand command) {
		scheduleValidator.requireUniqueSchedule(
			command.tenantId(), command.stackId(), command.teamId(), command.sampledAt());
		scheduleValidator.requireMeasurersInTenant(
			command.mentorId(), command.menteeId(), command.tenantId());

		Schedule saved = scheduleRepository.save(Schedule.register(
			command.tenantId(), command.stackId(), command.teamId(), command.mentorId(), command.menteeId(),
			command.measurementField(), command.schedulePurpose(), command.referenceNumber(), command.sampledAt()
		));

		ScheduleSnapshot snapshot = snapshotAssembler.assemble(saved, command.pollutantIds());
		ScheduleSnapshot savedSnapshot = scheduleDocumentRepository.save(snapshot);
		return new ScheduleDetail(saved, savedSnapshot);
	}

	/**
	 * 계획을 정의하는 값(채취일자·측정용도·관리번호)을 수정한다. 측정정보 탭이 단독으로 소유해
	 * 폼이 자기 필드 전부를 보내므로 <b>전체 채택</b>이다 — 빈 칸은 "지웠다"는 뜻이다
	 * (단 채취일자는 DB NOT NULL이자 집계 기준일이라 null이면 유지한다).
	 * <p>
	 * 한 메서드에 두 시맨틱을 섞으면 자기 것이 아닌 칸에 null을 실은 호출자가 남의 값을 지운다.
	 */
	public ScheduleDetail updateMeta(Long id, Long tenantId, UpdateScheduleCommand command) {
		Schedule schedule = scheduleRepository.findById(id, tenantId);
		schedule.requireEditable();

		Schedule saved = scheduleRepository.save(schedule.updateMetadata(
			command.sampledAt(), command.schedulePurpose(), command.referenceNumber()
		));

		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
		return new ScheduleDetail(saved, snapshot);
	}

	/**
	 * 성적서를 진행하며 채우는 일자 셋(시료접수·분석완료·성적서발행)을 수정한다. 실험·분석 탭이 이 셋을
	 * <b>단독으로 소유</b>하므로 <b>전체 채택</b>이다 — 빈 칸은 "지웠다"는 뜻이고, 잘못 넣은 일자를 비울 수 있다.
	 * <p>
	 * 일자는 메타에만 있어 문서에 되비출 것이 없으므로 <b>문서를 쓰지 않는다</b> — 이유 없는 문서 쓰기는
	 * 낙관적 락만 건드린다. 다만 시료접수일이 채워지면 분석 착수로 보고 상태를 전진시켜야 하므로
	 * 문서를 읽어 판정에 넘긴다({@link ScheduleProgress}).
	 */
	public ScheduleDetail updateReportDates(Long id, Long tenantId, UpdateReportDatesCommand command) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		meta.requireEditable();

		Schedule saved = scheduleRepository.save(meta.applyReportProgress(
			command.receivedAt(), command.analyzedAt(), command.issuedAt()
		));

		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
		return statusTransitioner.advanceAfterDocumentSaved(saved, snapshot);
	}

	/** 분석을 마친 측정계획을 완료로 확정한다. 이후 편집이 잠기며 측정 건수 통계에 집계된다. */
	public ScheduleDetail complete(Long id, Long tenantId) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		return statusTransitioner.confirmTransition(meta, meta.complete());
	}

	/**
	 * 업무가 무산된 측정계획을 취소한다. 삭제와 달리 계획은 목록에 남는다.
	 * 완료·취소된 계획은 다시 취소할 수 없다.
	 */
	public ScheduleDetail cancel(Long id, Long tenantId) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		return statusTransitioner.confirmTransition(meta, meta.cancel());
	}

	/**
	 * 종단 상태(완료·취소)를 풀어 다시 작업할 수 있게 한다.
	 * <p>
	 * 돌아갈 단계는 스냅샷에서 재도출한다 — 실측값이 있으면 측정 중, 시료접수일까지 있으면 분석값 입력 중이다.
	 * 취소는 세 단계 어디에서든 걸 수 있어 되돌릴 지점이 하나로 정해지지 않는데, 진행 단계가 원래
	 * 스냅샷에서 파생되는 값이므로 같은 답이 나온다.
	 * 재개방하면 상태가 완료가 아니게 되므로 측정 건수 통계에서도 자동으로 빠진다.
	 */
	public ScheduleDetail reopen(Long id, Long tenantId) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);

		Schedule cleared = meta.reopen();
		return statusTransitioner.confirmTransition(meta, ScheduleProgress.advance(cleared, snapshot), snapshot);
	}

	/**
	 * 잘못 등록된 측정계획을 지운다. 되돌릴 수 없으므로 실측 데이터가 없는 측정 예정·취소에서만
	 * 가능하며, 진행 중인 계획은 취소로 처리해야 한다.
	 * 메타(MySQL)와 세부 문서(MongoDB)를 함께 지우며, 삭제 순서는 저장과 반대로 Mongo → MySQL이다.
	 */
	public void deleteSchedule(Long id, Long tenantId) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		meta.requireDeletable();

		// 삭제 가능한 상태(측정 예정·취소)에는 완료 이력이 있을 수 없다. 그래도 지우는 이유는,
		// 이력 백필처럼 상태와 기록이 어긋날 수 있는 경로가 있어 지운 계획의 이행이 현황판에 남지 않게 하기 위해서다.
		measurementRecordRecorder.revoke(id, tenantId);
		// 실험분석정보는 문서 안(items[].analysis)에 있으므로 문서 삭제가 곧 분석 결과 삭제다.
		scheduleDocumentRepository.deleteByScheduleId(id, tenantId);
		scheduleRepository.deleteById(id, tenantId);
	}

	@Transactional(readOnly = true)
	public ScheduleDetail getSchedule(Long id, Long tenantId) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
		return new ScheduleDetail(meta, snapshot);
	}

	@Transactional(readOnly = true)
	public List<ScheduleListItem> getScheduleList(Long tenantId) {
		return toListItems(
			scheduleRepository.findAll(tenantId).stream()
				.filter(schedule -> schedule.getStatus() != ScheduleStatus.CANCELED)
				.toList(),
			tenantId);
	}

	/** 취소된 측정계획 목록을 반환한다. */
	@Transactional(readOnly = true)
	public List<ScheduleListItem> getCanceledScheduleList(Long tenantId) {
		List<Schedule> canceled = scheduleRepository.findAll(tenantId).stream()
			.filter(schedule -> schedule.getStatus() == ScheduleStatus.CANCELED)
			.toList();

		return toListItems(canceled, tenantId);
	}

	/** 메타 목록에 세부 문서를 조인해 목록 아이템으로 조립한다. */
	private List<ScheduleListItem> toListItems(List<Schedule> metas, Long tenantId) {
		Map<Long, ScheduleSnapshot> snapshotByScheduleId = scheduleDocumentRepository.findAll(tenantId).stream()
			.collect(Collectors.toMap(ScheduleSnapshot::scheduleId, Function.identity()));

		return metas.stream()
			.map(meta -> scheduleListItemMapper.toListItem(meta, snapshotByScheduleId.get(meta.getId())))
			.toList();
	}
}
