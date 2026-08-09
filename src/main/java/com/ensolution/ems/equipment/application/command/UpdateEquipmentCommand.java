package com.ensolution.ems.equipment.application.command;

import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.equipment.domain.InspectionItemChange;
import com.ensolution.ems.equipment.domain.spec.EquipmentSpec;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateEquipmentCommand(
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
	List<InspectionItemChange> inspectionChanges,
	EquipmentSpec spec
) {
}
