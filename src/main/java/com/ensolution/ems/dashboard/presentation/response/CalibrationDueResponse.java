package com.ensolution.ems.dashboard.presentation.response;

import java.time.LocalDate;

/** 교정일 임박 장비 응답 항목. {@code daysRemaining}은 교정 기한이 지났으면 음수다. */
public record CalibrationDueResponse(
	String equipmentId,
	String equipmentName,
	String managementNumber,
	LocalDate nextCalibrationDate,
	long daysRemaining
) {
}
