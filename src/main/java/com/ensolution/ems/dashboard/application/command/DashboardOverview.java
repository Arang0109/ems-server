package com.ensolution.ems.dashboard.application.command;

import java.util.List;

/**
 * 대시보드 상단 KPI 요약 VO. 공급 모듈(client_management·contract·schedule)의 인바운드 포트 집계 결과를 조합한다.
 * {@code DashboardOverviewResponse}와 필드가 1:1로 대응한다.
 *
 * @param workplaceCount             사업장 수
 * @param contractCount              계약 건수
 * @param stackCount                 측정시설(굴뚝) 수
 * @param completedMeasurementCount  완료 측정 총 건수
 * @param thisMonthMeasurementCount  이번 달 완료 측정 건수
 * @param newContractCount           이번 달 신규 계약 건수(계약일 기준)
 * @param expiringContracts          만료 임박 계약 목록(임박한 순)
 * @param inspectionDueEquipments    검사 임박 장비 목록(장비-검사항목 단위, 임박한 순)
 */
public record DashboardOverview(
	long workplaceCount,
	long contractCount,
	long stackCount,

	long completedMeasurementCount,
	long thisMonthMeasurementCount,
	long newContractCount,

	List<ExpiringContract> expiringContracts,
	List<InspectionDue> inspectionDueEquipments
) {
}
