package com.ensolution.ems.schedule.application.validator;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.port.out.ScheduleRepository;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** 측정계획 비즈니스 규칙 검증. 포트 조회가 필요한 규칙만 담당한다. */
@Component
@RequiredArgsConstructor
public class ScheduleValidator {

	private final ScheduleRepository scheduleRepository;

	/** 같은 측정시설·팀·채취일자로 등록된 계획이 없는지 확인한다. */
	public void requireUniqueSchedule(Long tenantId, Long stackId, Long teamId, LocalDate sampledAt) {
		if (scheduleRepository.existsByStackIdAndTeamIdAndMeasureDate(tenantId, stackId, teamId, sampledAt)) {
			throw new CustomException(ErrorCode.SCHEDULE_ALREADY_EXISTS);
		}
	}

	/**
	 * 측정항목 순서 변경 요청이 이 계획의 측정항목 집합과 정확히 일치하는지 확인한다.
	 * <p>
	 * 중복·누락·이 계획에 없는 물질을 저장 전에 한 번에 잡아 부분 저장을 막는다.
	 * 부분 목록을 허용하지 않는 이유는 순서가 집합 전체에 대한 전순서이기 때문이다 —
	 * 일부만 재배열하면 나머지 항목이 어디에 놓이는지 정의되지 않는다.
	 * 내가 화면을 연 뒤 다른 사용자가 측정항목을 교체한 경우도 이 규칙이 함께 잡아낸다.
	 * <p>
	 * 서비스가 이미 읽어온 스냅샷과 대조하므로 포트를 재조회하지 않는다.
	 */
	public void requireExactItemOrder(List<SamplingItemSnapshot> current, List<Long> orderedPollutantIds) {
		if (Set.copyOf(orderedPollutantIds).size() != orderedPollutantIds.size()) {
			throw new CustomException(ErrorCode.SCHEDULE_ITEM_ORDER_MISMATCH);
		}

		Set<Long> currentIds = (current == null ? List.<SamplingItemSnapshot>of() : current).stream()
			.filter(Objects::nonNull)
			.map(SamplingItemSnapshot::pollutantId)
			.collect(Collectors.toSet());

		if (!currentIds.equals(Set.copyOf(orderedPollutantIds))) {
			throw new CustomException(ErrorCode.SCHEDULE_ITEM_ORDER_MISMATCH);
		}
	}
}
