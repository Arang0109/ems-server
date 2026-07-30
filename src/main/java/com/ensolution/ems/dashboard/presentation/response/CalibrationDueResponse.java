package com.ensolution.ems.dashboard.presentation.response;

public record CalibrationDueResponse(
	String equipmentName,
	long daysRemaining
) {
}
