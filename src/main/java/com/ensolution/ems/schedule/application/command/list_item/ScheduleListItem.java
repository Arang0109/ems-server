package com.ensolution.ems.schedule.application.command.list_item;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.schedule.domain.ScheduleStatus;

import java.time.LocalDateTime;

/**
 * 측정계획 목록 조회 아이템. 표시용 이름(clientName·stackName·teamName)은 세부 스냅샷에서 추출한다.
 */
public record ScheduleListItem(
	Long id,
	Long stackId,
	Long teamId,
	MeasurementField measurementField,
	LocalDateTime measureDate,
	String measurementType,
	ScheduleStatus status,
	String referenceNumber,
	String clientName,
	String stackName,
	String teamName,
	LocalDateTime createdAt
) {}
