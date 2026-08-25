package com.ensolution.ems.equipment.presentation.response;

import com.ensolution.ems.equipment.domain.InspectionResult;
import com.ensolution.ems.equipment.domain.InspectionType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record InspectionRecordResponse(
	String id,
	String equipmentId,
	InspectionType type,
	String typeLabel,
	LocalDate inspectedAt,
	LocalDate validUntil,
	String agency,
	String certificateNumber,
	InspectionResult result,
	String remark,
	LocalDateTime createdAt
) {}
