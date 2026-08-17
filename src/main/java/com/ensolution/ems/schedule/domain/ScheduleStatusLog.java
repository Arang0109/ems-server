package com.ensolution.ems.schedule.domain;

import java.time.LocalDateTime;

/**
 * 측정계획 상태 변경 이력. "누가 언제 왜 상태를 바꿨는가"를 한 행으로 남긴다.
 * <p>
 * 취소·완료·재개방 시각과 사유를 메타 컬럼으로 따로 두지 않는 이유는 두 가지다.
 * 상태가 늘 때마다 컬럼이 세 개씩 붙고, 무엇보다 재개방을 허용하면 완료 → 재개방 → 완료로
 * 상태가 왕복하므로 단일 컬럼으로는 이력을 표현할 수 없다.
 *
 * @param fromStatus 직전 상태. 최초 등록 시에는 없으므로 null이다.
 * @param reason     변경 사유. 취소·재개방은 필수이고 자동 전이는 null이다.
 * @param automatic  스냅샷 입력에 의한 자동 전이 여부. true면 changedBy가 null이다.
 * @param changedBy  변경한 사용자 id. 자동 전이는 null이다.
 */
public record ScheduleStatusLog(
	Long id,
	Long scheduleId,
	Long tenantId,
	ScheduleStatus fromStatus,
	ScheduleStatus toStatus,
	String reason,
	boolean automatic,
	Long changedBy,
	LocalDateTime changedAt
) {
	/** 사용자가 명시적으로 확정한 전이(완료·취소·재개방)를 기록한다. */
	public static ScheduleStatusLog manual(Long scheduleId, Long tenantId,
	                                       ScheduleStatus fromStatus, ScheduleStatus toStatus,
	                                       String reason, Long changedBy) {
		return new ScheduleStatusLog(null, scheduleId, tenantId, fromStatus, toStatus,
			reason, false, changedBy, LocalDateTime.now());
	}

	/** 스냅샷 입력에 따라 서버가 전진시킨 전이(측정 착수·분석 착수)를 기록한다. */
	public static ScheduleStatusLog automatic(Long scheduleId, Long tenantId,
	                                          ScheduleStatus fromStatus, ScheduleStatus toStatus) {
		return new ScheduleStatusLog(null, scheduleId, tenantId, fromStatus, toStatus,
			null, true, null, LocalDateTime.now());
	}
}
