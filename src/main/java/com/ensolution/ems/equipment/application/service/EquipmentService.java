package com.ensolution.ems.equipment.application.service;

import com.ensolution.ems.equipment.application.command.CreateEquipmentCommand;
import com.ensolution.ems.equipment.application.command.UpdateEquipmentCommand;
import com.ensolution.ems.equipment.application.port.in.EquipmentQueryUseCase;
import com.ensolution.ems.equipment.application.port.in.EquipmentSummary;
import com.ensolution.ems.equipment.application.port.out.EquipmentRepository;
import com.ensolution.ems.equipment.domain.EquipStatus;
import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.equipment.domain.Equipment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipmentService implements EquipmentQueryUseCase {

	private final EquipmentRepository equipmentRepository;

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
			command.calibrationCycle(),
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
			command.calibrationCycle(),
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
			equipment.getCalibrationCycle(),
			equipment.getLastCalibrationDate(),
			equipment.getSpec()
		);
	}
}
