package com.ensolution.ems.equipment.application.service;

import com.ensolution.ems.equipment.application.command.CreateEquipmentCommand;
import com.ensolution.ems.equipment.application.command.RecordInspectionCommand;
import com.ensolution.ems.equipment.application.command.UpdateEquipmentCommand;
import com.ensolution.ems.equipment.application.mapper.InspectionDueSummaryMapper;
import com.ensolution.ems.equipment.application.port.in.EquipmentQueryUseCase;
import com.ensolution.ems.equipment.application.port.in.EquipmentSummary;
import com.ensolution.ems.equipment.application.port.in.InspectionDueSummary;
import com.ensolution.ems.equipment.application.port.out.EquipmentRepository;
import com.ensolution.ems.equipment.application.port.out.InspectionRecordRepository;
import com.ensolution.ems.equipment.domain.EquipStatus;
import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.equipment.domain.Equipment;
import com.ensolution.ems.equipment.domain.InspectionRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipmentService implements EquipmentQueryUseCase {

	private final EquipmentRepository equipmentRepository;
	private final InspectionRecordRepository inspectionRecordRepository;
	private final InspectionDueSummaryMapper inspectionDueSummaryMapper;

	public Equipment createEquipment(CreateEquipmentCommand command) {
		Equipment equipment = Equipment.register(
			command.tenantId(),
			command.type(),
			command.managementNumber(),
			command.serialNumber(),
			command.modelName(),
			command.equipmentName(),
			command.alias(),
			command.price(),
			command.manufacturer(),
			command.originCountry(),
			command.purchaseDate(),
			command.remark(),
			command.inspections(),
			command.spec()
		);
		return equipmentRepository.save(equipment);
	}

	public Equipment updateEquipment(String equipmentId, Long tenantId, UpdateEquipmentCommand command) {
		Equipment equipment = equipmentRepository.findById(equipmentId, tenantId);

		Equipment updated = equipment.update(
			command.type(),
			command.managementNumber(),
			command.serialNumber(),
			command.modelName(),
			command.equipmentName(),
			command.alias(),
			command.price(),
			command.manufacturer(),
			command.originCountry(),
			command.purchaseDate(),
			command.remark(),
			command.inspectionChanges(),
			command.spec()
		);
		return equipmentRepository.save(updated);
	}

	public Equipment changeStatus(String equipmentId, Long tenantId, EquipStatus status) {
		Equipment equipment = equipmentRepository.findById(equipmentId, tenantId);
		return equipmentRepository.save(equipment.changeStatus(status));
	}

	public void deleteEquipment(String equipmentId, Long tenantId) {
		Equipment equipment = equipmentRepository.findById(equipmentId, tenantId);
		equipmentRepository.save(equipment.delete());
	}

	public Equipment getEquipment(String equipmentId, Long tenantId) {
		return equipmentRepository.findById(equipmentId, tenantId);
	}

	public List<Equipment> getEquipmentList(EquipType type, Long tenantId) {
		if (type == null) {
			return equipmentRepository.findAll(tenantId);
		}
		return equipmentRepository.findByType(type, tenantId);
	}

	/**
	 * 검사 실시를 기록하고 장비의 해당 검사 항목 최종 수검일을 갱신한다.
	 * <p>
	 * MongoDB standalone에는 문서 간 트랜잭션이 없으므로 이력을 먼저 저장한다.
	 * 중간에 실패하면 "이력은 남았고 수검일만 미반영" 상태가 되어 재입력 없이 복구할 수 있지만,
	 * 반대 순서라면 근거 이력 없이 수검일만 미뤄진 장비가 남는다.
	 */
	public InspectionRecord recordInspection(RecordInspectionCommand command) {
		Equipment equipment = equipmentRepository.findById(command.equipmentId(), command.tenantId());
		// 이력을 남기기 전에 검사 대상인지 먼저 확인한다. 그러지 않으면 거부된 요청의 이력만 남는다.
		equipment.requireInspectionEnabled(command.type());

		InspectionRecord saved = inspectionRecordRepository.save(InspectionRecord.register(
			command.tenantId(),
			command.equipmentId(),
			command.type(),
			command.inspectedAt(),
			command.validUntil(),
			command.agency(),
			command.certificateNumber(),
			command.result(),
			command.remark()
		));

		equipmentRepository.save(
			equipment.recordInspection(command.type(), command.inspectedAt(), command.validUntil())
		);
		return saved;
	}

	public List<InspectionRecord> getInspectionRecords(String equipmentId, Long tenantId) {
		equipmentRepository.findById(equipmentId, tenantId); // tenant 소유권 확인
		return inspectionRecordRepository.findByEquipmentId(equipmentId, tenantId);
	}

	@Override
	public EquipmentSummary getEquipmentSummary(String equipmentId, Long tenantId) {
		Equipment equipment = equipmentRepository.findById(equipmentId, tenantId);
		return new EquipmentSummary(
			equipment.getId(),
			equipment.getType(),
			equipment.getManagementNumber(),
			equipment.getSerialNumber(),
			equipment.getModelName(),
			equipment.getEquipmentName(),
			equipment.getAlias(),
			equipment.getManufacturer(),
			equipment.getInspections(),
			equipment.getSpec()
		);
	}

	@Override
	public List<InspectionDueSummary> findInspectionDueBefore(Long tenantId, LocalDate dueDate) {
		return equipmentRepository.findAll(tenantId).stream()
			.filter(equipment -> equipment.getStatus() == EquipStatus.ACTIVE)
			.flatMap(equipment -> equipment.notifiableItemsDueBefore(dueDate).stream()
				.map(item -> inspectionDueSummaryMapper.toInspectionDueSummary(equipment, item)))
			.sorted(Comparator.comparing(InspectionDueSummary::nextDueDate))
			.toList();
	}
}
