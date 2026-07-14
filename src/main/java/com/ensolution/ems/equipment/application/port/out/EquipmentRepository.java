package com.ensolution.ems.equipment.application.port.out;

import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.equipment.domain.Equipment;

import java.util.List;

public interface EquipmentRepository {
	Equipment save(Equipment equipment);
	Equipment findById(String id, Long tenantId);
	List<Equipment> findAll(Long tenantId);
	List<Equipment> findByType(EquipType type, Long tenantId);
}
