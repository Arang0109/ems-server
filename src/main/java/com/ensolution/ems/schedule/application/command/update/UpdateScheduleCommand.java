package com.ensolution.ems.schedule.application.command.update;

import com.ensolution.ems.global.common.enums.MeasurementField;

import java.time.LocalDateTime;

/** 측정계획 메타 수정 커맨드. 대상(stackId·teamId)은 변경 대상이 아니다. */
public record UpdateScheduleCommand(
	MeasurementField measurementField,
	LocalDateTime measureDate,
	String measurementType,
	String referenceNumber
) {}
