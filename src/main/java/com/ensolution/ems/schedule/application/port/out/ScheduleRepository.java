package com.ensolution.ems.schedule.application.port.out;

import com.ensolution.ems.schedule.domain.Schedule;

import java.time.LocalDate;
import java.util.List;

/** 측정계획 메타(MySQL) 아웃바운드 포트. 삭제는 행을 지우는 물리 삭제다. */
public interface ScheduleRepository {
	Schedule save(Schedule schedule);
	Schedule findById(Long id, Long tenantId);
	List<Schedule> findAll(Long tenantId);
	boolean existsByStackIdAndTeamIdAndMeasureDate(Long tenantId, Long stackId, Long teamId, LocalDate measureDate);
	void deleteById(Long id, Long tenantId);

	/**
	 * 같은 측정시설에서 기준일보다 앞서 완료된 계획을 최신순으로 최대 {@code limit}건 반환한다.
	 * 이전 기록지를 찾을 때 쓰며, 회차마다 쓰는 기록지가 달라 한 건만으로는 부족하므로 여러 건을 받는다.
	 * 해당하는 계획이 없으면 빈 목록이다(첫 회차는 정상 상황이므로 예외가 아니다).
	 */
	List<Schedule> findRecentCompletedBefore(Long stackId, Long tenantId, LocalDate before, int limit);
}
