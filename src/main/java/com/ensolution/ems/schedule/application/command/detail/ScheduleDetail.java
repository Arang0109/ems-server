package com.ensolution.ems.schedule.application.command.detail;

import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;

/**
 * 측정계획 상세 조회 결과. 메타(Schedule)와 세부 스냅샷(ScheduleSnapshot)을 조립한 VO.
 */
public record ScheduleDetail(
	Schedule meta,
	ScheduleSnapshot snapshot
) {}
