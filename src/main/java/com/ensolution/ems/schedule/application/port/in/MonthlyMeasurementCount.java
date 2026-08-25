package com.ensolution.ems.schedule.application.port.in;

/**
 * 특정 월의 완료 측정 건수. 대시보드 등 타 모듈에 월별 측정 통계를 제공하기 위한 공개 VO.
 *
 * @param month 1~12
 * @param count 해당 월의 REPORT_COMPLETED 측정계획 건수(없으면 0)
 */
public record MonthlyMeasurementCount(int month, long count) {
}
