package com.ensolution.ems.dashboard.application.service;

import com.ensolution.ems.dashboard.application.command.DashboardOverview;
import com.ensolution.ems.dashboard.application.command.MeasurementCountItem;
import com.ensolution.ems.schedule.application.port.in.ScheduleStatisticsUseCase;
import com.ensolution.ems.client_management.application.port.in.StackQueryUseCase;
import com.ensolution.ems.client_management.application.port.in.WorkplaceQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 대시보드 통계 조립 유스케이스. 자체 원장을 두지 않고 공급 모듈(tenant·schedule)의
 * 인바운드 포트만 조합하는 순수 조회 서비스다(크로스모듈 접근은 port/in 경유).
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

	private final WorkplaceQueryUseCase workplaceQueryUseCase;
	private final StackQueryUseCase stackQueryUseCase;
	private final ScheduleStatisticsUseCase scheduleStatisticsUseCase;

	/** 상단 KPI 요약(사업장·측정시설 수, 총·이번달 측정 건수)을 조립한다. */
	public DashboardOverview getOverview(Long tenantId) {
		return new DashboardOverview(
			workplaceQueryUseCase.countWorkplaces(tenantId),
			stackQueryUseCase.countStacks(tenantId),
			scheduleStatisticsUseCase.countCompleted(tenantId),
			scheduleStatisticsUseCase.countCompletedInMonth(tenantId, YearMonth.now())
		);
	}

	/** 올해 1~12월 완료 측정건수 추이를 조립한다(데이터 없는 달은 0). */
	public List<MeasurementCountItem> getMeasurementStats(Long tenantId) {
		int year = LocalDate.now().getYear();
		return scheduleStatisticsUseCase.monthlyCompletedCounts(tenantId, year).stream()
			.map(monthly -> new MeasurementCountItem(monthly.month() + "월", monthly.count()))
			.toList();
	}
}
