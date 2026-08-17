package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.command.create.CreateScheduleCommand;
import com.ensolution.ems.schedule.application.command.detail.ScheduleDetail;
import com.ensolution.ems.schedule.application.command.list_item.ScheduleListItem;
import com.ensolution.ems.schedule.application.command.update.ChangeScheduleEquipmentsCommand;
import com.ensolution.ems.schedule.application.command.update.ChangeClientSnapshotCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateBasicInfoCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateScheduleCommand;
import com.ensolution.ems.schedule.application.port.in.MonthlyMeasurementCount;
import com.ensolution.ems.schedule.application.port.in.ScheduleStatisticsUseCase;
import com.ensolution.ems.schedule.application.port.out.ScheduleDocumentRepository;
import com.ensolution.ems.schedule.application.port.out.ScheduleRepository;
import com.ensolution.ems.schedule.application.validator.ScheduleValidator;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.ScheduleProgress;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.domain.ScheduleStatusLog;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import com.ensolution.ems.schedule.domain.snapshot.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 측정계획 유스케이스. 메타(MySQL)를 진실의 원천으로 두고, 세부 스냅샷(MongoDB) 저장/삭제를
 * 각 트랜잭션의 마지막 부수효과로 배치해 정합성을 확보한다(2PC 불가).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService implements ScheduleStatisticsUseCase {

	private final ScheduleRepository scheduleRepository;
	private final ScheduleDocumentRepository scheduleDocumentRepository;
	private final ScheduleSnapshotAssembler snapshotAssembler;
	private final SnapshotSheetRecalculator recalculator;
	private final ScheduleStatusRecorder statusRecorder;
	private final ScheduleValidator scheduleValidator;

	public ScheduleDetail createSchedule(CreateScheduleCommand command) {
		scheduleValidator.requireUniqueActiveSchedule(
			command.tenantId(), command.stackId(), command.teamId(), command.sampledAt());

		Schedule saved = scheduleRepository.save(Schedule.register(
			command.tenantId(),
			command.stackId(),
			command.teamId(),
			command.measurementField(),
			command.sampledAt(),
			command.schedulePurpose(),
			command.referenceNumber()
		));

		statusRecorder.recordRegistration(saved, command.registeredBy());

		ScheduleSnapshot snapshot = snapshotAssembler.assemble(saved, command.pollutantIds());
		ScheduleSnapshot savedSnapshot = scheduleDocumentRepository.save(snapshot);
		return new ScheduleDetail(saved, savedSnapshot);
	}

	public ScheduleDetail updateSchedule(Long id, Long tenantId, UpdateScheduleCommand command) {
		Schedule schedule = scheduleRepository.findById(id, tenantId);
		schedule.requireEditable();

		Schedule saved = scheduleRepository.save(schedule.update(
			command.measurementField(),
			command.sampledAt(),
			command.schedulePurpose(),
			command.referenceNumber()
		));

		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
		// 측정·성적서 발행 단계에서 채워지는 값(담당자·접수/분석/발행일자·채취 시각)이
		// 메타 수정으로 유실되지 않도록 기존 기본 정보에 병합한다.
		BasicInfo basicInfo = basicInfoOf(snapshot, saved).applyMeta(
			saved.getReferenceNumber(),
			saved.getSampledAt(),
			saved.getMeasurementField(),
			saved.getSchedulePurpose());
		ScheduleSnapshot savedSnapshot = scheduleDocumentRepository.save(
			snapshot.applyMetaUpdate(basicInfo, command.tenant()));
		return new ScheduleDetail(saved, savedSnapshot);
	}

	/**
	 * 측정계획 문서의 기본 정보를 수정한다. 담당자(배출시설관리자·시료채취입회자·시료분석검사자·기술책임자)·
	 * 시료접수/분석완료/성적서발행일자·채취 시작/종료 시각과 측정자 표기를 부분 갱신하며,
	 * 전달되지 않은 필드는 기존 값을 유지한다.
	 * 계산 입력이 아닌 값만 다루므로 측정 시트는 재계산하지 않는다. 원장(tenant)은 변경하지 않으며,
	 * 완료·취소된 계획은 수정할 수 없다.
	 * 갱신 결과에 따른 자동 전이(접수일자 입력 → 분석 중 등)는 {@link ScheduleProgress}가 판정한다.
	 */
	public ScheduleDetail updateBasicInfo(Long id, Long tenantId, UpdateBasicInfoCommand command) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		meta.requireEditable();

		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
		BasicInfo basicInfo = basicInfoOf(snapshot, meta).update(
			command.facilityManager(),
			command.samplingWitness(),
			command.analyst(),
			command.technicalManager(),
			command.receivedAt(),
			command.analyzedAt(),
			command.issuedAt(),
			command.samplingStartedAt(),
			command.samplingEndedAt()
		);
		TeamSnapshot team = snapshot.team() == null
			? null
			: snapshot.team().withMembers(command.mentorName(), command.menteeName());

		ScheduleSnapshot updated = snapshot.applyBasicInfo(basicInfo, team);
		return saveAdvanced(meta, ScheduleProgress.advance(meta, updated), updated);
	}

	/** 문서의 기본 정보를 반환한다. 기본 정보 없이 저장된 과거 문서는 메타에서 새로 조립한다. */
	private BasicInfo basicInfoOf(ScheduleSnapshot snapshot, Schedule meta) {
		if (snapshot.basicInfo() != null) return snapshot.basicInfo();
		return BasicInfo.create(
			meta.getReferenceNumber(),
			meta.getSampledAt(),
			meta.getMeasurementField(),
			meta.getSchedulePurpose(),
			null,
			null);
	}

	/** 분석을 마친 측정계획을 완료로 확정한다. 이후 편집이 잠기며 측정 건수 통계에 집계된다. */
	public ScheduleDetail complete(Long id, Long tenantId, Long userId) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		return applyStatusChange(meta, meta.complete(), null, userId);
	}

	/**
	 * 업무가 무산된 측정계획을 취소한다. 삭제와 달리 계획은 목록에 남고 사유가 이력으로 기록된다.
	 * 완료·취소된 계획은 다시 취소할 수 없다.
	 */
	public ScheduleDetail cancel(Long id, Long tenantId, Long userId, String reason) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		return applyStatusChange(meta, meta.cancel(), reason, userId);
	}

	/**
	 * 종단 상태(완료·취소)를 풀어 다시 작업할 수 있게 한다.
	 * <p>
	 * 돌아갈 단계는 스냅샷에서 재도출한다 — 실측값이 있으면 측정 중, 시료접수일까지 있으면 분석 중이다.
	 * 취소는 세 단계 어디에서든 걸 수 있어 되돌릴 지점이 하나로 정해지지 않는데, 진행 단계가 원래
	 * 스냅샷에서 파생되는 값이므로 이력을 뒤지지 않아도 같은 답이 나온다.
	 * 완료를 되돌리는 경우에만 관리자 권한이 필요하며, 재개방하면 상태가 완료가 아니게 되므로
	 * 측정 건수 통계에서도 자동으로 빠진다.
	 */
	public ScheduleDetail reopen(Long id, Long tenantId, Long userId, String reason, boolean hasAdminPrivilege) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);

		Schedule cleared = meta.reopen(hasAdminPrivilege);
		return applyStatusChange(meta, ScheduleProgress.advance(cleared, snapshot), reason, userId, snapshot);
	}

	/** 사용자가 확정한 상태 전이를 저장하고 이력을 남긴다. 저장 순서는 MySQL → Mongo다. */
	private ScheduleDetail applyStatusChange(Schedule meta, Schedule changed, String reason, Long userId) {
		return applyStatusChange(meta, changed, reason, userId,
			scheduleDocumentRepository.findByScheduleId(meta.getId(), meta.getTenantId()));
	}

	private ScheduleDetail applyStatusChange(
		Schedule meta, Schedule changed, String reason, Long userId, ScheduleSnapshot snapshot
	) {
		Schedule saved = scheduleRepository.save(changed);
		statusRecorder.recordManual(meta, saved, reason, userId);

		ScheduleSnapshot savedSnapshot = scheduleDocumentRepository.save(snapshot.syncStatus(saved.getStatus()));
		return new ScheduleDetail(saved, savedSnapshot);
	}

	/**
	 * 자동 전이 결과를 반영해 저장한다. 상태가 실제로 바뀐 경우에만 메타(MySQL)를 저장하고 스냅샷 상태를
	 * 동기화하므로, 전이가 없는 일반 저장은 문서 단독 쓰기로 남는다.
	 * 저장 순서는 {@link #applyStatusChange}와 동일하게 MySQL → Mongo다.
	 */
	private ScheduleDetail saveAdvanced(Schedule meta, Schedule advanced, ScheduleSnapshot snapshot) {
		if (advanced.getStatus() == meta.getStatus()) {
			return new ScheduleDetail(meta, scheduleDocumentRepository.save(snapshot));
		}

		Schedule saved = scheduleRepository.save(advanced);
		statusRecorder.recordAutomatic(meta, saved);
		ScheduleSnapshot savedSnapshot = scheduleDocumentRepository.save(snapshot.syncStatus(saved.getStatus()));
		return new ScheduleDetail(saved, savedSnapshot);
	}

	/**
	 * 잘못 등록된 측정계획을 감춘다(soft delete). 실측 데이터가 없는 '측정 예정'에서만 가능하며,
	 * 측정에 착수한 계획은 취소로 처리해야 한다.
	 * 세부 문서(MongoDB)는 복구를 위해 지우지 않는다 — 메타가 조회에서 걸러지므로 노출되지 않는다.
	 */
	public void deleteSchedule(Long id, Long tenantId, Long userId) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		scheduleRepository.save(meta.delete(userId));
	}

	/**
	 * 감춰진 측정계획을 되살린다(관리자 전용). 삭제 시점의 상태를 그대로 회복한다.
	 * 삭제 후 같은 조건으로 다시 등록한 계획이 있으면 자리가 겹치므로 복구할 수 없다.
	 */
	public ScheduleDetail restore(Long id, Long tenantId) {
		Schedule meta = scheduleRepository.findByIdIncludingDeleted(id, tenantId);
		scheduleValidator.requireRestorable(meta);

		Schedule saved = scheduleRepository.save(meta.restore());
		return new ScheduleDetail(saved, scheduleDocumentRepository.findByScheduleId(id, tenantId));
	}

	/** 삭제된 측정계획 목록을 반환한다(관리자 전용). */
	@Transactional(readOnly = true)
	public List<ScheduleListItem> getDeletedScheduleList(Long tenantId) {
		return toListItems(scheduleRepository.findAllDeleted(tenantId), tenantId);
	}

	/** 측정계획의 상태 변경 이력을 시간순으로 반환한다. */
	@Transactional(readOnly = true)
	public List<ScheduleStatusLog> getStatusLogs(Long id, Long tenantId) {
		return statusRecorder.findLogs(id, tenantId);
	}

	/**
	 * 측정 시트를 저장한다. 저장 시 계산 파이프라인을 실행해 계산 결과가 반영된 시트를 함께 저장한다.
	 * 완료·취소된 계획은 편집할 수 없다. 피토관 계수는 스냅샷의 팀 장비에서, 표준산소농도는 각 시트 입력에서 취한다.
	 * 측정점에 실측값이 들어오면 측정 착수로 보고 상태를 전진시킨다(사용자의 별도 조작이 필요 없다).
	 * 시트 틀만 저장하는 경로는 착수로 보지 않으며, 판정은 {@link ScheduleProgress}가 담당한다.
	 */
	public ScheduleDetail saveSheets(Long id, Long tenantId, List<MeasurementSheet> sheets) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		meta.requireEditable();

		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
		ScheduleSnapshot recalculated = snapshot.withSheets(recalculator.recalculate(snapshot, sheets));

		return saveAdvanced(meta, ScheduleProgress.advance(meta, recalculated), recalculated);
	}

	/**
	 * 측정계획의 측정장비를 교체한다. 전달되지 않은 슬롯은 기존 장비를 유지하며, 팀 스냅샷의 장비 id와
	 * 장비 목록을 함께 갱신하고 기존 시트를 새 장비 spec으로 재계산한다.
	 * 장비는 메타(MySQL)가 아닌 문서(MongoDB)에만 존재하므로 상태 전이가 없는 한 문서 단독 쓰기이며,
	 * 원장(tenant·equipment)은 변경하지 않는다. 완료·취소된 계획은 변경할 수 없다.
	 */
	public ScheduleDetail changeEquipments(Long id, Long tenantId, ChangeScheduleEquipmentsCommand command) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		meta.requireEditable();

		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
		TeamSnapshot newTeam = snapshot.team().withEquipmentIds(
			command.particleSamplerId(),
			command.gasSamplerId(),
			command.pitotTubeId(),
			command.nozzleId()
		);
		List<EquipmentSnapshot> newEquipments = snapshotAssembler.resolveEquipments(newTeam, tenantId);

		// 재계산은 교체 후 스냅샷을 입력으로 해야 새 피토관 계수·노즐경이 반영된다.
		ScheduleSnapshot changed = snapshot.applyEquipmentChange(newTeam, newEquipments, snapshot.sheets());
		ScheduleSnapshot recalculated = changed.withSheets(recalculator.recalculate(changed, changed.sheets()));

		return saveAdvanced(meta, ScheduleProgress.advance(meta, recalculated), recalculated);
	}

	/**
	 * 측정계획 문서의 의뢰기관(→사업장→측정시설) 스냅샷을 수정한다. 전달되지 않은 필드는 기존 값을 유지하며,
	 * 측정시설의 표준산소농도·굴뚝 형상 등은 계산 입력이므로 기존 시트를 새 값으로 재계산한다.
	 * 원장(tenant Client·Workplace·Stack)은 변경하지 않는다. 완료·취소된 계획은 변경할 수 없다.
	 */
	public ScheduleDetail changeClient(Long id, Long tenantId, ChangeClientSnapshotCommand command) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		meta.requireEditable();

		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
		ClientSnapshot patch = new ClientSnapshot(
			null,                      // clientId는 원장 연결키이므로 변경 대상이 아니다.
			command.name(),
			command.bizNumber(),
			command.representative(),
			command.roadAddress(),
			command.detailAddress(),
			command.zipcode(),
			command.email(),
			command.tel(),
			command.workplace()
		);

		// 재계산은 병합 후 스냅샷을 입력으로 해야 새 표준산소농도·굴뚝 형상이 반영된다.
		ScheduleSnapshot changed = snapshot.applyClientChange(patch, snapshot.sheets());
		ScheduleSnapshot recalculated = changed.withSheets(recalculator.recalculate(changed, changed.sheets()));

		return saveAdvanced(meta, ScheduleProgress.advance(meta, recalculated), recalculated);
	}

	@Transactional(readOnly = true)
	public ScheduleDetail getSchedule(Long id, Long tenantId) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
		return new ScheduleDetail(meta, snapshot);
	}

	@Transactional(readOnly = true)
	public List<ScheduleListItem> getScheduleList(Long tenantId) {
		return toListItems(scheduleRepository.findAll(tenantId), tenantId);
	}

	/** 메타 목록에 세부 문서를 조인해 목록 아이템으로 조립한다. */
	private List<ScheduleListItem> toListItems(List<Schedule> metas, Long tenantId) {
		Map<Long, ScheduleSnapshot> snapshotByScheduleId = scheduleDocumentRepository.findAll(tenantId).stream()
			.collect(Collectors.toMap(ScheduleSnapshot::scheduleId, Function.identity(), (a, b) -> a));

		return metas.stream()
			.map(meta -> toListItem(meta, snapshotByScheduleId.get(meta.getId())))
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public long countCompleted(Long tenantId) {
		return scheduleRepository.findAll(tenantId).stream()
			.filter(ScheduleService::isCompleted)
			.count();
	}

	@Override
	@Transactional(readOnly = true)
	public long countCompletedInMonth(Long tenantId, YearMonth yearMonth) {
		return scheduleRepository.findAll(tenantId).stream()
			.filter(ScheduleService::isCompleted)
			.filter(schedule -> yearMonth.equals(YearMonth.from(schedule.getSampledAt())))
			.count();
	}

	@Override
	@Transactional(readOnly = true)
	public List<MonthlyMeasurementCount> monthlyCompletedCounts(Long tenantId, int year) {
		Map<Integer, Long> countByMonth = scheduleRepository.findAll(tenantId).stream()
			.filter(ScheduleService::isCompleted)
			.filter(schedule -> schedule.getSampledAt().getYear() == year)
			.collect(Collectors.groupingBy(
				schedule -> schedule.getSampledAt().getMonthValue(),
				Collectors.counting()));

		return IntStream.rangeClosed(1, 12)
			.mapToObj(month -> new MonthlyMeasurementCount(month, countByMonth.getOrDefault(month, 0L)))
			.toList();
	}

	/** 완료 상태이면서 집계 기준일(measureDate)을 가진 측정계획인지 여부. */
	private static boolean isCompleted(Schedule schedule) {
		return schedule.getStatus() == ScheduleStatus.COMPLETED && schedule.getSampledAt() != null;
	}

	private ScheduleListItem toListItem(Schedule meta, ScheduleSnapshot snapshot) {
		return new ScheduleListItem(
			meta.getId(),
			meta.getStackId(),
			meta.getTeamId(),
			meta.getMeasurementField(),
			meta.getSampledAt(),
			meta.getSchedulePurpose(),
			meta.getStatus(),
			meta.getReferenceNumber(),
			clientName(snapshot),
			stackName(snapshot),
			teamName(snapshot),
			meta.getCreatedAt(),
			meta.getDeletedAt(),
			meta.getDeletedBy()
		);
	}

	private String clientName(ScheduleSnapshot snapshot) {
		if (snapshot == null || snapshot.client() == null) return null;
		return snapshot.client().name();
	}

	private String stackName(ScheduleSnapshot snapshot) {
		if (snapshot == null) return null;
		ClientSnapshot client = snapshot.client();
		if (client == null || client.workplace() == null || client.workplace().stack() == null) return null;
		return client.workplace().stack().name();
	}

	private String teamName(ScheduleSnapshot snapshot) {
		if (snapshot == null || snapshot.team() == null) return null;
		return snapshot.team().teamName();
	}
}
