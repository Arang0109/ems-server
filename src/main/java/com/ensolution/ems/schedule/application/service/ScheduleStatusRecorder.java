package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.port.out.ScheduleStatusLogRepository;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.ScheduleStatusLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 측정계획 상태 변경 이력을 기록한다.
 * 이력 기록은 모든 상태 변경 경로(자동 전이·완료·취소·재개방)에서 반복되므로
 * {@link ScheduleService}에서 분리해 둔다. 호출하는 서비스의 트랜잭션에 참여한다.
 */
@Component
@RequiredArgsConstructor
public class ScheduleStatusRecorder {

	private final ScheduleStatusLogRepository scheduleStatusLogRepository;

	/**
	 * 상태가 실제로 바뀐 경우에만 자동 전이 이력을 남긴다.
	 * 전이가 없는 저장에서 호출해도 안전하므로 호출부에서 조건을 따질 필요가 없다.
	 */
	public void recordAutomatic(Schedule before, Schedule after) {
		if (before.getStatus() == after.getStatus()) return;

		scheduleStatusLogRepository.save(ScheduleStatusLog.automatic(
			after.getId(), after.getTenantId(), before.getStatus(), after.getStatus()));
	}

	/** 사용자가 확정한 전이(완료·취소·재개방) 이력을 사유와 함께 남긴다. */
	public void recordManual(Schedule before, Schedule after, String reason, Long changedBy) {
		scheduleStatusLogRepository.save(ScheduleStatusLog.manual(
			after.getId(), after.getTenantId(), before.getStatus(), after.getStatus(), reason, changedBy));
	}

	/** 최초 등록 이력을 남긴다(직전 상태 없음). */
	public void recordRegistration(Schedule registered, Long changedBy) {
		scheduleStatusLogRepository.save(ScheduleStatusLog.manual(
			registered.getId(), registered.getTenantId(), null, registered.getStatus(), null, changedBy));
	}

	public List<ScheduleStatusLog> findLogs(Long scheduleId, Long tenantId) {
		return scheduleStatusLogRepository.findByScheduleId(scheduleId, tenantId);
	}
}
