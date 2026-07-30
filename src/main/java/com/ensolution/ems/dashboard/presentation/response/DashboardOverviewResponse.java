package com.ensolution.ems.dashboard.presentation.response;

import java.util.List;

/** 대시보드 상단 KPI 요약 응답. 프론트 대시보드 카드 4종에 1:1 매핑된다. */
public record DashboardOverviewResponse(
	long workplaceCount,							// 사업장 수
	long contractCount,								// 계약 건수
	long stackCount,									// 측정지점 수
	
	long completedMeasurementCount,		// 총 측정건수(완료)
	long thisMonthMeasurementCount,		// 이번달 측정건수(완료)
	long newContractCount, 						// 신규 계약 건수
	
	List<ExpiringContractResponse> expiringContracts, 	// 계약 만료 예정 리스트
	List<CalibrationDueResponse> calibrationEquipments	// 교정 장비 알림
) {
}