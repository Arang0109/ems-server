package com.ensolution.ems.schedule.application.validator;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.port.out.ScheduleRepository;
import com.ensolution.ems.schedule.domain.Schedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** 측정계획 비즈니스 규칙 검증. 포트 조회가 필요한 규칙만 담당한다. */
@Component
@RequiredArgsConstructor
public class ScheduleValidator {

	private final ScheduleRepository scheduleRepository;

	/**
	 * 같은 측정시설·팀·채취일자로 등록된 활성 계획이 없는지 확인한다.
	 * 삭제된 계획은 세지 않으므로, 잘못 등록해 지운 조합으로 다시 등록할 수 있다.
	 */
	public void requireUniqueActiveSchedule(Long tenantId, Long stackId, Long teamId, LocalDate sampledAt) {
		if (scheduleRepository.existsByStackIdAndTeamIdAndMeasureDate(tenantId, stackId, teamId, sampledAt)) {
			throw new CustomException(ErrorCode.SCHEDULE_ALREADY_EXISTS);
		}
	}

	/**
	 * 복구하려는 계획의 자리가 비어 있는지 확인한다.
	 * 삭제 후 같은 조건으로 다시 등록한 계획이 있으면 복구 시 유니크 제약에 걸리므로,
	 * DB 오류(500) 대신 의미 있는 충돌(409)로 알린다.
	 */
	public void requireRestorable(Schedule deleted) {
		requireUniqueActiveSchedule(
			deleted.getTenantId(), deleted.getStackId(), deleted.getTeamId(), deleted.getSampledAt());
	}
}
