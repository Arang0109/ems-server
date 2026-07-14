package com.ensolution.ems.equipment.presentation.response;

import com.ensolution.ems.equipment.domain.EquipStatus;
import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.equipment.domain.spec.EquipmentSpec;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EquipmentResponse(
	String id,
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
	LocalDate lastCalibrationDate,
	EquipStatus status,
	EquipmentSpec spec,
	LocalDateTime createdAt,
	LocalDateTime modifiedAt
) {}
