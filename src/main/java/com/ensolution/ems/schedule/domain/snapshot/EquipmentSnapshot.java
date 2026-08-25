package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.equipment.domain.InspectionItem;
import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.equipment.domain.spec.EquipmentSpec;

import java.util.List;

/**
 * 측정 시점 장비 스냅샷. 장비 유형별 사양은 equipment 도메인의 {@link EquipmentSpec}(sealed)을 그대로 보관하며,
 * Spring Data MongoDB가 {@code _class} 판별자로 구현체를 복원한다.
 * 검사 항목({@link InspectionItem})도 equipment 도메인 record를 그대로 보관한다.
 */
public record EquipmentSnapshot(
	String equipmentId,
	EquipType type,
	String managementNumber,
	String serialNumber,
	String modelName,
	String equipmentName,
	String alias,
	String manufacturer,
	List<InspectionItem> inspections,
	EquipmentSpec spec
) {}
