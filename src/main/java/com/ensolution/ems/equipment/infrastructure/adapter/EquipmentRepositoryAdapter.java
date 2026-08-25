package com.ensolution.ems.equipment.infrastructure.adapter;

import com.ensolution.ems.equipment.application.port.out.EquipmentRepository;
import com.ensolution.ems.equipment.domain.EquipStatus;
import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.equipment.domain.Equipment;
import com.ensolution.ems.equipment.infrastructure.document.EquipmentDocument;
import com.ensolution.ems.equipment.infrastructure.mapper.EquipmentDocumentMapper;
import com.ensolution.ems.equipment.infrastructure.repository.EquipmentMongoRepository;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EquipmentRepositoryAdapter implements EquipmentRepository {

	private final EquipmentMongoRepository equipmentMongoRepository;
	private final EquipmentDocumentMapper mapper;

	@Override
	public Equipment save(Equipment equipment) {
		EquipmentDocument saved = equipmentMongoRepository.save(mapper.toDocument(equipment));
		return mapper.toDomain(saved);
	}

	@Override
	public Equipment findById(String id, Long tenantId) {
		return equipmentMongoRepository.findByIdAndTenantIdAndStatusNot(id, tenantId, EquipStatus.DELETED)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.EQUIPMENT_NOT_FOUND));
	}

	@Override
	public List<Equipment> findAll(Long tenantId) {
		return mapper.toDomains(
			equipmentMongoRepository.findByTenantIdAndStatusNot(tenantId, EquipStatus.DELETED)
		);
	}

	@Override
	public List<Equipment> findByType(EquipType type, Long tenantId) {
		return mapper.toDomains(
			equipmentMongoRepository.findByTenantIdAndTypeAndStatusNot(tenantId, type, EquipStatus.DELETED)
		);
	}
}
