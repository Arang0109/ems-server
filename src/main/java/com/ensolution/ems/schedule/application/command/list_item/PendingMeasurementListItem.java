package com.ensolution.ems.schedule.application.command.list_item;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.schedule.domain.history.FulfillmentStatus;

import java.time.LocalDate;

/**
 * 아직 이행하지 않은 주기 구간 하나. 현황판 셀을 조치가 필요한 것만 골라 펼친 것이다.
 *
 * @param daysRemaining 기한까지 남은 일수. <b>기한이 지났으면 음수</b>이며, 목록은 이 값 오름차순이라
 *                      경과 건이 앞에 온다({@code equipment} 검사 임박 목록과 같은 규칙)
 */
public record PendingMeasurementListItem(
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
