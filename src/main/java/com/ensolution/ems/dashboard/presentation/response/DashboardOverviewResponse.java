package com.ensolution.ems.dashboard.presentation.response;

/** 대시보드 상단 KPI 요약 응답. 프론트 대시보드 카드 4종에 1:1 매핑된다. */
public record DashboardOverviewResponse(
	long workplaceCount,
	long stackCount,
	long totalMeasurements,
	long thisMonthMeasurements
) {
}
