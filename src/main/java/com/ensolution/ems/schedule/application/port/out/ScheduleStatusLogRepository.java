package com.ensolution.ems.schedule.application.port.out;

import com.ensolution.ems.schedule.domain.ScheduleStatusLog;

import java.util.List;

/** 측정계획 상태 변경 이력(MySQL) 아웃바운드 포트. 이력은 추가 전용이며 수정·삭제하지 않는다. */
public interface ScheduleStatusLogRepository {
	ScheduleStatusLog save(ScheduleStatusLog log);

	/** 변경 시각 오름차순으로 이력을 반환한다. */
	List<ScheduleStatusLog> findByScheduleId(Long scheduleId, Long tenantId);
}
