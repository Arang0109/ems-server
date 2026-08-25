package com.ensolution.ems.dashboard.application.service;

import com.ensolution.ems.client_management.application.port.in.StackQueryUseCase;
import com.ensolution.ems.client_management.application.port.in.WorkplaceQueryUseCase;
import com.ensolution.ems.contract.application.port.in.ContractStatisticsUseCase;
import com.ensolution.ems.dashboard.application.command.DashboardOverview;
import com.ensolution.ems.dashboard.application.command.ExpiringContract;
import com.ensolution.ems.dashboard.application.command.InspectionDue;
import com.ensolution.ems.dashboard.application.command.MeasurementCountItem;
import com.ensolution.ems.dashboard.application.mapper.ContractPortMapper;
import com.ensolution.ems.dashboard.application.mapper.EquipmentPortMapper;
import com.ensolution.ems.equipment.application.port.in.EquipmentQueryUseCase;
import com.ensolution.ems.schedule.application.port.in.ScheduleStatisticsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 대시보드 통계 조립 유스케이스. 자체 원장을 두지 않고 공급 모듈(client_management·contract·equipment·schedule)의
 * 인바운드 포트만 조합하는 순수 조회 서비스다(크로스모듈 접근은 port/in 경유).
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

	/** 완료일이 이 개월 수 이내로 남은 계약을 "만료 임박"으로 본다. */
	private static final int EXPIRY_WITHIN_MONTHS = 2;

	/** 검사 예정일이 이 개월 수 이내인 검사 항목을 "검사 임박"으로 본다(기한이 지난 항목도 포함). */
	private static final int INSPECTION_WITHIN_MONTHS = 2;

	private final WorkplaceQueryUseCase workplaceQueryUseCase;
	private final StackQueryUseCase stackQueryUseCase;
	private final ScheduleStatisticsUseCase scheduleStatisticsUseCase;
	private final ContractStatisticsUseCase contractStatisticsUseCase;
	private final EquipmentQueryUseCase equipmentQueryUseCase;

	private final ContractPortMapper contractPortMapper;
	private final EquipmentPortMapper equipmentPortMapper;

	/** 상단 KPI 요약(사업장·계약·측정시설 수, 측정 건수, 만료 임박 계약, 검사 임박 장비)을 조립한다. */
	@Transactional(readOnly = true)
	public DashboardOverview getOverview(Long tenantId) {
		LocalDate today = LocalDate.now();
		YearMonth thisMonth = YearMonth.now();

		return new DashboardOverview(
			workplaceQueryUseCase.countWorkplaces(tenantId),
			contractStatisticsUseCase.countContracts(tenantId),
			stackQueryUseCase.countStacks(tenantId),

			scheduleStatisticsUseCase.countCompleted(tenantId),
			scheduleStatisticsUseCase.countCompletedInMonth(tenantId, thisMonth),
			contractStatisticsUseCase.countContractsInMonth(tenantId, thisMonth),

			expiringContracts(tenantId, today),
			inspectionDueEquipments(tenantId, today)
		);
	}

	/** 올해 1~12월 완료 측정건수 추이를 조립한다(데이터 없는 달은 0). */
	public List<MeasurementCountItem> getMeasurementStats(Long tenantId) {
		int year = LocalDate.now().getYear();
		return scheduleStatisticsUseCase.monthlyCompletedCounts(tenantId, year).stream()
			.map(monthly -> new MeasurementCountItem(monthly.month() + "월", monthly.count()))
			.toList();
	}

	private List<ExpiringContract> expiringContracts(Long tenantId, LocalDate today) {
		return contractPortMapper.toExpiringContracts(
			contractStatisticsUseCase.findExpiringBetween(tenantId, today, today.plusMonths(EXPIRY_WITHIN_MONTHS)),
			today
		);
	}

	private List<InspectionDue> inspectionDueEquipments(Long tenantId, LocalDate today) {
		return equipmentPortMapper.toInspectionDues(
			equipmentQueryUseCase.findInspectionDueBefore(tenantId, today.plusMonths(INSPECTION_WITHIN_MONTHS)),
			today
		);
	}
}
