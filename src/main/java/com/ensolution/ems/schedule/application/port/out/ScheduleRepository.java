package com.ensolution.ems.schedule.application.port.out;

import com.ensolution.ems.schedule.domain.Schedule;

import java.time.LocalDateTime;
import java.util.List;

/** 측정계획 메타(MySQL) 아웃바운드 포트. */
public interface ScheduleRepository {
	Schedule save(Schedule schedule);
	Schedule findById(Long id, Long tenantId);
	List<Schedule> findAll(Long tenantId);
	boolean existsByStackIdAndTeamIdAndMeasureDate(Long stackId, Long teamId, LocalDateTime measureDate);
	void deleteById(Long id, Long tenantId);
}
