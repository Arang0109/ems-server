package com.ensolution.ems.schedule.application.command.detail;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.schedule.domain.history.FulfillmentStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * 연간 주기 이행 현황판. 행은 측정항목(측정시설별), 열은 그 항목의 주기 구간이다.
 * 주기가 다른 항목은 열 개수도 다르므로(월 12칸, 분기 4칸) 격자가 아니라 행마다 자기 열을 갖는다.
 *
 * @param unscheduledItemCount 주기가 지정되지 않아 이행 추적에서 빠진 항목 수.
 *                             행에서 조용히 사라지면 "왜 안 보이지"가 되므로 개수를 함께 알린다
 */
public record FulfillmentBoardDetail(
	int year,
	int unscheduledItemCount,
	List<Row> rows
) {

	/**
	 * @param requiredTotal  연간 필요 횟수 합계(구간 수 × 구간당 필요 횟수)
	 * @param fulfilledTotal 연간 이행 횟수 합계. 필요 횟수를 넘겨 측정한 구간도 실제 횟수로 센다
	 */
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

	/**
	 * @param dueDate     구간 종료일 = 이행 기한
	 * @param scheduleIds 이 구간을 이행한 측정계획들. 셀에서 해당 회차로 이동하는 데 쓴다
	 */
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
