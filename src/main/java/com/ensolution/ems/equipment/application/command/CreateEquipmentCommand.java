package com.ensolution.ems.equipment.application.command;

import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.equipment.domain.spec.EquipmentSpec;

import java.math.BigDecimal;
import java.time.LocalDate;

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
	Integer calibrationCycle,
	EquipmentSpec spec
) {
}
