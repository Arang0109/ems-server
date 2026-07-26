package com.ensolution.ems.schedule.presentation.response;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.schedule.domain.ScheduleStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 측정계획 목록 응답 아이템. */
public record ScheduleListResponse(
	Long id,
	Long stackId,
	Long teamId,
	MeasurementField measurementField,
	LocalDate sampledAt,
	String schedulePurpose,
	ScheduleStatus status,
	String referenceNumber,
	String clientName,
	String stackName,
	String teamName,
	LocalDateTime createdAt
) {}
