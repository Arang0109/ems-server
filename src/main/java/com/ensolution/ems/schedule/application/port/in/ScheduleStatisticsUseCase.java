package com.ensolution.ems.schedule.application.port.in;

import java.time.YearMonth;
import java.util.List;

/**
 * 측정계획 통계를 타 모듈(대시보드 등)에 제공하는 인바운드 포트.
 * "측정 건수"의 도메인 규칙(COMPLETED 상태만 집계, measureDate 기준)은 소유 모듈인 schedule이 정의한다.
 * 모든 조회는 소유권 격리를 위해 tenantId 범위 안에서만 집계한다.
 */
public interface ScheduleStatisticsUseCase {

	/** 완료(COMPLETED) 측정계획 전체 건수. */
	long countCompleted(Long tenantId);

	/** measureDate가 주어진 연·월에 속하는 완료 측정계획 건수. */
	long countCompletedInMonth(Long tenantId, YearMonth yearMonth);

	/** 주어진 연도의 1~12월별 완료 측정계획 건수(데이터 없는 달은 0). */
	List<MonthlyMeasurementCount> monthlyCompletedCounts(Long tenantId, int year);
}
