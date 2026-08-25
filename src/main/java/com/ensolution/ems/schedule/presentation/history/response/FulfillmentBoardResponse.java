package com.ensolution.ems.schedule.presentation.history.response;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.schedule.domain.history.FulfillmentStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * 연간 주기 이행 현황판 응답. 행은 측정항목, 열은 그 항목의 주기 구간이다.
 * 주기마다 구간 수가 다르므로(월 12, 분기 4) 행마다 {@code cells} 길이가 다르다.
 */
public record FulfillmentBoardResponse(
	int year,
	int unscheduledItemCount,
	List<Row> rows
) {

	public record Row(
		Long workplaceId,
		String workplaceName,
		Long stackId,
		String stackName,
		Long stackPollutantId,
		Long pollutantId,
		String code,
		String nameKr,
		MeasurementCycle cycle,
		int requiredTotal,
		int fulfilledTotal,
		List<Cell> cells
	) {}

	public record Cell(
		int index,
		String key,
		String label,
		LocalDate dueDate,
		int requiredCount,
		int fulfilledCount,
		FulfillmentStatus status,
		List<Long> scheduleIds
	) {}
}
