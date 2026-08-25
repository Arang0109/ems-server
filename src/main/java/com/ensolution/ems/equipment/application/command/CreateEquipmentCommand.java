package com.ensolution.ems.equipment.application.command;

import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.equipment.domain.InspectionItem;
import com.ensolution.ems.equipment.domain.spec.EquipmentSpec;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateEquipmentCommand(
	Long tenantId,
	EquipType type,
	String managementNumber,
	String serialNumber,
	String modelName,
	String equipmentName,
	String alias,
	BigDecimal price,
	String manufacturer,
	String originCountry,
	LocalDate purchaseDate,
	String remark,
	/** 비어 있으면 장비 유형별 기본 검사 세트가 주입된다. */
	List<InspectionItem> inspections,
	EquipmentSpec spec
) {
}
