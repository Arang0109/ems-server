package com.ensolution.ems.dashboard.presentation.response;

import com.ensolution.ems.equipment.domain.InspectionType;

import java.time.LocalDate;

/** 검사 임박 장비 응답 항목. {@code daysRemaining}은 검사 기한이 지났으면 음수다. */
public record InspectionDueResponse(
	String equipmentId,
	String equipmentName,
	String managementNumber,
	InspectionType inspectionType,
	String inspectionTypeLabel,
	LocalDate nextDueDate,
	long daysRemaining
) {
}
