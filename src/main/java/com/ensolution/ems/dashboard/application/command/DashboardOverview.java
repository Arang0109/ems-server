package com.ensolution.ems.dashboard.application.command;

/**
 * 대시보드 상단 KPI 요약 VO. 공급 모듈(tenant·schedule)의 인바운드 포트 집계 결과를 조합한다.
 *
 * @param workplaceCount        사업장 수
 * @param stackCount            측정시설(굴뚝) 수
 * @param totalMeasurements     완료 측정 총 건수
 * @param thisMonthMeasurements 이번 달 완료 측정 건수
 */
public record DashboardOverview(
	long workplaceCount,
	long stackCount,
	long totalMeasurements,
	long thisMonthMeasurements
) {
}
