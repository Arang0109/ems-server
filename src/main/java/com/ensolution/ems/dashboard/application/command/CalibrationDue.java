package com.ensolution.ems.dashboard.application.command;

import java.time.LocalDate;

/** 교정일 임박 장비 항목. {@code daysRemaining}은 조회 기준일부터 교정 예정일까지의 잔여일수로, 기한이 지났으면 음수다. */
public record CalibrationDue(
	String equipmentId,
	String equipmentName,
	String managementNumber,
	LocalDate nextCalibrationDate,
	long daysRemaining
) {
}
