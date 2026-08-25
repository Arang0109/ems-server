package com.ensolution.ems.schedule.presentation.history.response;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.schedule.domain.history.FulfillmentStatus;

import java.time.LocalDate;

/** 미이행·기한임박 구간 목록 응답. {@code daysRemaining}이 음수면 기한이 지난 구간이다. */
public record PendingMeasurementListResponse(
	Long workplaceId,
	String workplaceName,
	Long stackId,
	String stackName,
	Long stackPollutantId,
	Long pollutantId,
	String code,
	String nameKr,
	MeasurementCycle cycle,
	String periodKey,
	String periodLabel,
	LocalDate dueDate,
	long daysRemaining,
	int requiredCount,
	int fulfilledCount,
	FulfillmentStatus status
) {}
