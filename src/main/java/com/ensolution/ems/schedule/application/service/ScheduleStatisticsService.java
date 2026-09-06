package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.port.in.MonthlyMeasurementSummary;
import com.ensolution.ems.schedule.application.port.in.ScheduleStatisticsUseCase;
import com.ensolution.ems.schedule.application.port.out.ScheduleRepository;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 측정 건수 통계. 이 모듈이 밖으로 여는 유일한 계약({@link ScheduleStatisticsUseCase})의 구현이며
 * {@code dashboard}가 유일한 소비자다.
 *
 * <p>측정계획 유스케이스와 분리해 둔 이유는 소비자가 다르기 때문이다 — 나머지 서비스는 이 모듈의
 * 컨트롤러가 쓰고, 여기만 타 모듈이 포트로 쓴다. 공개 계약의 구현체가 어디인지 한 파일로 드러난다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleStatisticsService implements ScheduleStatisticsUseCase {

	private final ScheduleRepository scheduleRepository;

	@Override
	public long countCompleted(Long tenantId) {
		return scheduleRepository.findAll(tenantId).stream()
			.filter(ScheduleStatisticsService::isCompleted)
			.count();
	}

	@Override
	public long countCompletedInMonth(Long tenantId, YearMonth yearMonth) {
		return scheduleRepository.findAll(tenantId).stream()
			.filter(ScheduleStatisticsService::isCompleted)
			.filter(schedule -> yearMonth.equals(YearMonth.from(schedule.getSampledAt())))
			.count();
	}

	@Override
	public List<MonthlyMeasurementSummary> monthlyCompletedCounts(Long tenantId, int year) {
		Map<Integer, Long> countByMonth = scheduleRepository.findAll(tenantId).stream()
			.filter(ScheduleStatisticsService::isCompleted)
			.filter(schedule -> schedule.getSampledAt().getYear() == year)
			.collect(Collectors.groupingBy(
				schedule -> schedule.getSampledAt().getMonthValue(),
				Collectors.counting()));

		return IntStream.rangeClosed(1, 12)
			.mapToObj(month -> new MonthlyMeasurementSummary(month, countByMonth.getOrDefault(month, 0L)))
			.toList();
	}

	/** 완료 상태이면서 집계 기준일(채취일자)을 가진 측정계획인지 여부. */
	private static boolean isCompleted(Schedule schedule) {
		return schedule.getStatus() == ScheduleStatus.REPORT_COMPLETED && schedule.getSampledAt() != null;
	}
}
