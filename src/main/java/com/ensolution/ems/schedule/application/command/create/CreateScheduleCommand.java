package com.ensolution.ems.schedule.application.command.create;

import com.ensolution.ems.global.common.enums.MeasurementField;

import java.time.LocalDateTime;

public record CreateScheduleCommand(
	Long tenantId,
	Long stackId,
	Long teamId,
	MeasurementField measurementField,
	LocalDateTime measureDate,
	String measurementType,
	String referenceNumber
) {}
