package com.ensolution.ems.schedule.application.port.out;

import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;

import java.util.List;

/** 측정계획 세부 스냅샷(MongoDB) 아웃바운드 포트. */
public interface ScheduleDocumentRepository {
	ScheduleSnapshot save(ScheduleSnapshot snapshot);
	ScheduleSnapshot findByScheduleId(Long scheduleId, Long tenantId);
	List<ScheduleSnapshot> findAll(Long tenantId);
	void deleteByScheduleId(Long scheduleId, Long tenantId);
}
