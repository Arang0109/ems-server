package com.ensolution.ems.schedule.application.command.create;

import com.ensolution.ems.global.common.enums.MeasurementField;

import java.time.LocalDate;
import java.util.List;

/** @param registeredBy 등록한 사용자 id. 상태 변경 이력의 최초 행에 기록된다. */
public record CreateScheduleCommand(
	Long tenantId,
	Long stackId,
	Long teamId,
	MeasurementField measurementField,
	LocalDate sampledAt,
	List<Long> pollutantIds,
	String schedulePurpose,
	String referenceNumber,
	Long registeredBy
) {}
