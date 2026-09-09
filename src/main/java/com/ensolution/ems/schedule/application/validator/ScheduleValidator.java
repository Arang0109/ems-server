package com.ensolution.ems.schedule.application.validator;

import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
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
	private final UserQueryUseCase userQueryUseCase;

	/** 같은 측정시설·팀·채취일자로 등록된 계획이 없는지 확인한다. */
	public void requireUniqueSchedule(Long tenantId, Long stackId, Long teamId, LocalDate sampledAt) {
		if (scheduleRepository.existsByStackIdAndTeamIdAndMeasureDate(tenantId, stackId, teamId, sampledAt)) {
			throw new CustomException(ErrorCode.SCHEDULE_ALREADY_EXISTS);
		}
	}

	/**
	 * 이 회차에 배정된 측정자(사수·부사수)가 요청 tenant의 사용자인지 확인한다.
	 * <p>
	 * 측정자는 팀 원장의 사수·부사수가 아니라 <b>테넌트 사용자 전체에서 고르는 값</b>이라
	 * 팀 소속으로는 검증되지 않는다. tenant 대조는 {@code UserQueryUseCase.getUser(userId, tenantId)}가
	 * 수행하며, 여기서는 auth의 {@code USER_NOT_FOUND}를 사수·부사수 문맥으로 바꿔 던진다
	 * (미존재와 타 tenant를 구분하지 않는 것은 멀티테넌시 규칙 그대로다).
	 * 미지정(null)은 통과한다 — 그 경우 성적서 표기가 팀 원장의 이름으로 채워진다.
	 * <p>
	 * {@code client_management}의 {@code TeamValidator}와 같은 형태이며, 동일인 검사도 같은 이유로
	 * 여기에 둔다 — 애그리거트의 상태가 아니라 <b>요청 두 필드의 관계</b>에 대한 규칙이다.
	 */
	public void requireMeasurersInTenant(Long mentorId, Long menteeId, Long tenantId) {
		if (mentorId != null && mentorId.equals(menteeId)) {
			throw new CustomException(ErrorCode.SCHEDULE_MEASURER_DUPLICATED);
		}
		requireUserInTenant(mentorId, tenantId, ErrorCode.SCHEDULE_MENTOR_NOT_FOUND);
		requireUserInTenant(menteeId, tenantId, ErrorCode.SCHEDULE_MENTEE_NOT_FOUND);
	}

	private void requireUserInTenant(Long userId, Long tenantId, ErrorCode notFound) {
		if (userId == null) return;
		try {
			userQueryUseCase.getUser(userId, tenantId);
		} catch (CustomException e) {
			throw new CustomException(notFound);
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
			.map(SamplingItemSnapshot::pollutantId)
			.collect(Collectors.toSet());

		if (!currentIds.equals(Set.copyOf(orderedPollutantIds))) {
			throw new CustomException(ErrorCode.SCHEDULE_ITEM_ORDER_MISMATCH);
		}
	}
}
