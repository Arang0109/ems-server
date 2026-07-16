package com.ensolution.ems.dashboard.presentation.response;

/** 월별 측정건수 추이 차트의 단일 데이터 포인트 응답. */
public record MeasurementCountChartResponse(
	String label,
	long count
) {
}
