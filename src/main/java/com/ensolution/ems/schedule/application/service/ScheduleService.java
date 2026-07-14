package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.equipment.domain.spec.PitotTubeSpec;
import com.ensolution.ems.schedule.application.calculation.SheetCalculator;
import com.ensolution.ems.schedule.application.command.create.CreateScheduleCommand;
import com.ensolution.ems.schedule.application.command.detail.ScheduleDetail;
import com.ensolution.ems.schedule.application.command.list_item.ScheduleListItem;
import com.ensolution.ems.schedule.application.command.update.UpdateScheduleCommand;
import com.ensolution.ems.schedule.application.port.out.ScheduleDocumentRepository;
import com.ensolution.ems.schedule.application.port.out.ScheduleRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import com.ensolution.ems.schedule.domain.snapshot.ClientSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.EquipmentSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 측정계획 유스케이스. 메타(MySQL)를 진실의 원천으로 두고, 세부 스냅샷(MongoDB) 저장/삭제를
 * 각 트랜잭션의 마지막 부수효과로 배치해 정합성을 확보한다(2PC 불가).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {

	private final ScheduleRepository scheduleRepository;
	private final ScheduleDocumentRepository scheduleDocumentRepository;
	private final ScheduleSnapshotAssembler snapshotAssembler;
	private final SheetCalculator sheetCalculator;

	public ScheduleDetail createSchedule(CreateScheduleCommand command) {
		if (scheduleRepository.existsByStackIdAndTeamIdAndMeasureDate(
			command.stackId(), command.teamId(), command.measureDate())) {
			throw new CustomException(ErrorCode.SCHEDULE_ALREADY_EXISTS);
		}

		Schedule saved = scheduleRepository.save(Schedule.register(
			command.tenantId(),
			command.stackId(),
			command.teamId(),
			command.measurementField(),
			command.measureDate(),
			command.measurementType(),
			command.referenceNumber()
		));

		ScheduleSnapshot snapshot = snapshotAssembler.assemble(saved);
		ScheduleSnapshot savedSnapshot = scheduleDocumentRepository.save(snapshot);
		return new ScheduleDetail(saved, savedSnapshot);
	}

	public ScheduleDetail updateSchedule(Long id, Long tenantId, UpdateScheduleCommand command) {
		Schedule schedule = scheduleRepository.findById(id, tenantId);
		schedule.requireEditable();

		Schedule saved = scheduleRepository.save(schedule.update(
			command.measurementField(),
			command.measureDate(),
			command.measurementType(),
			command.referenceNumber()
		));
		ScheduleSnapshot snapshot = scheduleDocumentRepository.findByScheduleId(id, tenantId);
		return new ScheduleDetail(saved, snapshot);
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
		List<PitotTubeSpec.PitotCoefficient> pitotCoefficients = resolvePitotCoefficients(snapshot);
		BigDecimal standardOxygen = resolveStandardOxygen(snapshot);

		List<MeasurementSheet> calculated = sheets.stream()
			.map(sheet -> sheetCalculator.calculate(sheet, standardOxygen, pitotCoefficients))
			.toList();

		ScheduleSnapshot savedSnapshot = scheduleDocumentRepository.save(snapshot.withSheets(calculated));
		return new ScheduleDetail(meta, savedSnapshot);
	}

	private List<PitotTubeSpec.PitotCoefficient> resolvePitotCoefficients(ScheduleSnapshot snapshot) {
		if (snapshot.equipments() == null) return null;
		return snapshot.equipments().stream()
			.map(EquipmentSnapshot::spec)
			.filter(spec -> spec instanceof PitotTubeSpec)
			.map(spec -> ((PitotTubeSpec) spec).coefficients())
			.filter(coefficients -> coefficients != null)
			.findFirst()
			.orElse(null);
	}

	/** 산소보정계수 계산 입력인 표준산소농도를 측정시설 스냅샷에서 가져온다. */
	private BigDecimal resolveStandardOxygen(ScheduleSnapshot snapshot) {
		ClientSnapshot client = snapshot.client();
		if (client == null || client.workplace() == null || client.workplace().stack() == null) return null;
		Integer standardOxygen = client.workplace().stack().standardOxygen();
		return standardOxygen == null ? null : BigDecimal.valueOf(standardOxygen);
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

	private ScheduleListItem toListItem(Schedule meta, ScheduleSnapshot snapshot) {
		return new ScheduleListItem(
			meta.getId(),
			meta.getStackId(),
			meta.getTeamId(),
			meta.getMeasurementField(),
			meta.getMeasureDate(),
			meta.getMeasurementType(),
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
