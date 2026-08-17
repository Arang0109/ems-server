package com.ensolution.ems.schedule.application.command.list_item;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.schedule.domain.ScheduleStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 측정계획 목록 조회 아이템. 표시용 이름(clientName·stackName·teamName)은 세부 스냅샷에서 추출한다.
 * {@code deletedAt}·{@code deletedBy}는 삭제된 계획 목록에서만 채워지고 일반 목록에서는 항상 null이다.
 */
public record ScheduleListItem(
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
	LocalDateTime createdAt,
	LocalDateTime deletedAt,
	Long deletedBy
) {}
