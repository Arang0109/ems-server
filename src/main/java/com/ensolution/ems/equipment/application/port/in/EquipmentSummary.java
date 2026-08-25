package com.ensolution.ems.equipment.application.port.in;

import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.equipment.domain.InspectionItem;
import com.ensolution.ems.equipment.domain.spec.EquipmentSpec;

import java.util.List;

/**
 * 타 모듈(schedule 등)이 측정 시점 장비 스냅샷을 구성할 때 사용하는 장비 요약 VO.
 * equipment 모듈의 외부 공개 계약이므로 {@code application/port/in}에 위치한다.
 */
public record EquipmentSummary(
	String id,
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
