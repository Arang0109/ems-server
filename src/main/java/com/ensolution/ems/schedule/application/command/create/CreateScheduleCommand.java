package com.ensolution.ems.schedule.application.command.create;

import com.ensolution.ems.global.common.enums.MeasurementField;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CreateScheduleCommand(
	Long tenantId,
	Long stackId,
	Long teamId,
	MeasurementField measurementField,
	LocalDate sampledAt,
	List<Long> pollutantIds,
	String schedulePurpose,
	String referenceNumber
) {}
