package com.ensolution.ems.schedule.application.port.out;

import com.ensolution.ems.schedule.domain.Schedule;

import java.time.LocalDate;
import java.util.List;

/**
 * 측정계획 메타(MySQL) 아웃바운드 포트.
 * 삭제는 soft delete이므로 물리 삭제 메서드가 없다 — {@link #save}로 처리한다.
 * 이름에 {@code IncludingDeleted}·{@code Deleted}가 없는 조회는 활성 건만 반환한다.
 */
public interface ScheduleRepository {
	Schedule save(Schedule schedule);
	Schedule findById(Long id, Long tenantId);
	List<Schedule> findAll(Long tenantId);
	boolean existsByStackIdAndTeamIdAndMeasureDate(Long tenantId, Long stackId, Long teamId, LocalDate measureDate);

	/** 삭제된 건까지 포함해 조회한다(복구 경로 전용). */
	Schedule findByIdIncludingDeleted(Long id, Long tenantId);

	/** 삭제된 건만 조회한다(관리자 목록 전용). */
	List<Schedule> findAllDeleted(Long tenantId);
}
