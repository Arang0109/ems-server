package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.command.create.CreateScheduleCommand;
import com.ensolution.ems.schedule.application.command.detail.ScheduleDetail;
import com.ensolution.ems.schedule.application.command.list_item.ScheduleListItem;
import com.ensolution.ems.schedule.application.command.update.ChangeScheduleEquipmentsCommand;
import com.ensolution.ems.schedule.application.command.update.ChangeClientSnapshotCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateScheduleCommand;
import com.ensolution.ems.schedule.application.port.in.MonthlyMeasurementCount;
import com.ensolution.ems.schedule.application.port.in.ScheduleStatisticsUseCase;
import com.ensolution.ems.schedule.application.port.out.ScheduleDocumentRepository;
import com.ensolution.ems.schedule.application.port.out.ScheduleRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
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

	public ScheduleDetail createSchedule(CreateScheduleCommand command) {
		if (scheduleRepository.existsByStackIdAndTeamIdAndMeasureDate(
			command.stackId(), command.teamId(), command.sampledAt())) {
			throw new CustomException(ErrorCode.SCHEDULE_ALREADY_EXISTS);
		}

		Schedule saved = scheduleRepository.save(Schedule.register(
			command.tenantId(),
			command.stackId(),
			command.teamId(),
			command.measurementField(),
			command.sampledAt(),
			command.schedulePurpose(),
			command.referenceNumber()
		));

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
		BasicInfo basicInfo = BasicInfo.fromMeta(
			saved.getReferenceNumber(),
			saved.getSampledAt(),
			saved.getMeasurementField(),
			saved.getSchedulePurpose());
		ScheduleSnapshot savedSnapshot = scheduleDocumentRepository.save(
			snapshot.applyMetaUpdate(basicInfo, command.tenant()));
		return new ScheduleDetail(saved, savedSnapshot);
	}

	public ScheduleDetail changeStatus(Long id, Long tenantId, ScheduleStatus next) {
		Schedule schedule = scheduleRepository.findById(id, tenantId);
		Schedule saved = scheduleRepository.save(schedule.changeStatus(next));

		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
		ScheduleSnapshot savedSnapshot = scheduleDocumentRepository.save(snapshot.syncStatus(next));
		return new ScheduleDetail(saved, savedSnapshot);
	}

	public void deleteSchedule(Long id, Long tenantId) {
		scheduleRepository.deleteById(id, tenantId);
		scheduleDocumentRepository.deleteByScheduleId(id, tenantId);
	}

	/**
	 * 측정 시트를 저장한다. 저장 시 계산 파이프라인을 실행해 계산 결과가 반영된 시트를 함께 저장한다.
	 * 완료·취소된 계획은 편집할 수 없다. 피토관 계수는 스냅샷의 팀 장비에서, 표준산소농도는 각 시트 입력에서 취한다.
	 */
	public ScheduleDetail saveSheets(Long id, Long tenantId, List<MeasurementSheet> sheets) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		meta.requireEditable();

		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
		ScheduleSnapshot savedSnapshot = scheduleDocumentRepository.save(
			snapshot.withSheets(recalculator.recalculate(snapshot, sheets)));
		return new ScheduleDetail(meta, savedSnapshot);
	}

	/**
	 * 측정계획의 측정장비를 교체한다. 전달되지 않은 슬롯은 기존 장비를 유지하며, 팀 스냅샷의 장비 id와
	 * 장비 목록을 함께 갱신하고 기존 시트를 새 장비 spec으로 재계산한다.
	 * 장비는 메타(MySQL)가 아닌 문서(MongoDB)에만 존재하므로 문서 단독 쓰기이며, 원장(tenant·equipment)은
	 * 변경하지 않는다. 완료·취소된 계획은 변경할 수 없다.
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
		ScheduleSnapshot savedSnapshot = scheduleDocumentRepository.save(
			changed.withSheets(recalculator.recalculate(changed, changed.sheets())));
		return new ScheduleDetail(meta, savedSnapshot);
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
			command.facilityManager(),
			command.samplingWitness(),
			command.email(),
			command.tel(),
			command.workplace()
		);

		// 재계산은 병합 후 스냅샷을 입력으로 해야 새 표준산소농도·굴뚝 형상이 반영된다.
		ScheduleSnapshot changed = snapshot.applyClientChange(patch, snapshot.sheets());
		ScheduleSnapshot savedSnapshot = scheduleDocumentRepository.save(
			changed.withSheets(recalculator.recalculate(changed, changed.sheets())));
		return new ScheduleDetail(meta, savedSnapshot);
	}

	@Transactional(readOnly = true)
	public ScheduleDetail getSchedule(Long id, Long tenantId) {
		Schedule meta = scheduleRepository.findById(id, tenantId);
		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
		return new ScheduleDetail(meta, snapshot);
	}

	@Transactional(readOnly = true)
	public List<ScheduleListItem> getScheduleList(Long tenantId) {
		Map<Long, ScheduleSnapshot> snapshotByScheduleId = scheduleDocumentRepository.findAll(tenantId).stream()
			.collect(Collectors.toMap(ScheduleSnapshot::scheduleId, Function.identity(), (a, b) -> a));

		return scheduleRepository.findAll(tenantId).stream()
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
			meta.getCreatedAt()
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
