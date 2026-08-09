package com.ensolution.ems.dashboard.application.command;

import com.ensolution.ems.equipment.domain.InspectionType;

import java.time.LocalDate;

/**
 * 검사 예정일이 임박한 장비-검사항목 항목.
 * 한 장비가 여러 검사에서 임박하면 검사 종류별로 여러 건이 나온다.
 * {@code daysRemaining}은 조회 기준일부터 예정일까지의 잔여일수로, 기한이 지났으면 음수다.
 */
public record InspectionDue(
	String equipmentId,
	String equipmentName,
	String managementNumber,
	InspectionType inspectionType,
	String inspectionTypeLabel,
	LocalDate nextDueDate,
	long daysRemaining
) {
}
