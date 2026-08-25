package com.ensolution.ems.equipment.presentation.response;

import com.ensolution.ems.equipment.domain.EquipStatus;
import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.equipment.domain.spec.EquipmentSpec;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
	/* 검사 종류 전체가 항상 내려간다. 대상 여부는 각 항목의 {@code enabled} 로 판단한다. */
	List<InspectionItemResponse> inspections,
	EquipStatus status,
	EquipmentSpec spec,
	LocalDateTime createdAt,
	LocalDateTime modifiedAt
) {}
