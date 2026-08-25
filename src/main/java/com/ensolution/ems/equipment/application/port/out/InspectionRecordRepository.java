package com.ensolution.ems.equipment.application.port.out;

import com.ensolution.ems.equipment.domain.InspectionRecord;

import java.util.List;

public interface InspectionRecordRepository {

	InspectionRecord save(InspectionRecord record);

	/** 장비의 검사 이력을 최근 수검일 순으로 반환한다. */
	List<InspectionRecord> findByEquipmentId(String equipmentId, Long tenantId);
}
