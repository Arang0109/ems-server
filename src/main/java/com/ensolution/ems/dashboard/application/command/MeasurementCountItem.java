package com.ensolution.ems.dashboard.application.command;

/**
 * 월별 측정건수 추이 차트의 단일 아이템 VO.
 *
 * @param label 기간 라벨(예: "1월")
 * @param count 해당 기간의 완료 측정 건수
 */
public record MeasurementCountItem(
	String label,
	long count
) {
}
