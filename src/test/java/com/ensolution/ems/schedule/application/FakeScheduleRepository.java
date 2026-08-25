package com.ensolution.ems.schedule.application;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.port.out.ScheduleRepository;
import com.ensolution.ems.schedule.domain.Schedule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 인메모리 {@link ScheduleRepository}. 삭제는 실제 어댑터와 동일하게 행을 지운다. */
public class FakeScheduleRepository implements ScheduleRepository {

	private final List<Schedule> schedules = new ArrayList<>();
	private long nextId = 1L;

	/** 테스트 픽스처 등록. */
	public Schedule given(Schedule schedule) {
		return save(schedule.getId() == null ? schedule.toBuilder().id(nextId++).build() : schedule);
	}

	@Override
	public Schedule save(Schedule schedule) {
		schedules.removeIf(s -> Objects.equals(s.getId(), schedule.getId()));
		schedules.add(schedule);
		return schedule;
	}

	@Override
	public Schedule findById(Long id, Long tenantId) {
		return schedules.stream()
			.filter(s -> Objects.equals(s.getId(), id) && Objects.equals(s.getTenantId(), tenantId))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
	}

	@Override
	public List<Schedule> findAll(Long tenantId) {
		return schedules.stream()
			.filter(s -> Objects.equals(s.getTenantId(), tenantId))
			.toList();
	}

	@Override
	public boolean existsByStackIdAndTeamIdAndMeasureDate(
		Long tenantId, Long stackId, Long teamId, LocalDate measureDate) {
		return schedules.stream()
			.filter(s -> Objects.equals(s.getTenantId(), tenantId))
			.anyMatch(s -> Objects.equals(s.getStackId(), stackId)
				&& Objects.equals(s.getTeamId(), teamId)
				&& Objects.equals(s.getSampledAt(), measureDate));
	}

	@Override
	public void deleteById(Long id, Long tenantId) {
		boolean removed = schedules.removeIf(
			s -> Objects.equals(s.getId(), id) && Objects.equals(s.getTenantId(), tenantId));
		if (!removed) {
			throw new CustomException(ErrorCode.SCHEDULE_NOT_FOUND);
		}
	}

	@Override
	public List<Schedule> findRecentCompletedBefore(Long stackId, Long tenantId, LocalDate before, int limit) {
		return List.of();
	}
}
