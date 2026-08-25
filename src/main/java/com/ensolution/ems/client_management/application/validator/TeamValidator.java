package com.ensolution.ems.client_management.application.validator;

import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
import com.ensolution.ems.auth.application.port.in.UserSummary;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.client_management.application.port.out.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 측정 팀(Team) 생성·수정 시의 비즈니스 규칙 검증을 담당한다.
 * 단건 존재·소유권 검증은 Adapter의 {@code findById(id, tenantId)}가 담당하므로 여기서 다루지 않는다.
 */
@Component
@RequiredArgsConstructor
public class TeamValidator {

	private final TeamRepository teamRepository;
	private final UserQueryUseCase userQueryUseCase;

	/**
	 * 팀명은 tenant 안에서 유일해야 한다.
	 * 부모 aggregate가 없는 전역 유일 체크이므로 tenantId를 함께 받는다.
	 */
	public void requireUniqueName(String name, Long tenantId) {
		if (teamRepository.existsByNameAndTenantId(name, tenantId)) {
			throw new CustomException(ErrorCode.CONFLICT);
		}
	}

	/**
	 * 사수·부사수 user가 존재하고 요청 tenant 소속인지 검증한다.
	 * 미존재·타 tenant 모두 동일한 도메인 NOT_FOUND로 은닉한다(멀티테넌시 규칙).
	 * <p>
	 * tenant 대조는 {@code UserQueryUseCase.getUser(userId, tenantId)}가 수행한다.
	 * 여기서는 auth의 USER_NOT_FOUND를 팀 문맥의 사수·부사수 NOT_FOUND로 바꿔 던지기만 한다.
	 */
	public void requireMemberInTenant(Long userId, Long tenantId, ErrorCode notFound) {
		try {
			userQueryUseCase.getUser(userId, tenantId);
		} catch (CustomException e) {
			throw new CustomException(notFound);
		}
	}

	/**
	 * 한 사용자는 하나의 팀에만 사수 또는 부사수로 배정될 수 있다.
	 * 로그인 응답의 소속 팀이 유일하게 결정되도록 보장하는 규칙이다.
	 *
	 * @param excludeTeamId 수정 대상 팀. 자기 자신은 중복 배정으로 보지 않는다. 생성 시에는 {@code null}
	 */
	public void requireNotAssignedToOtherTeam(Long userId, Long tenantId, Long excludeTeamId) {
		boolean assignedElsewhere = teamRepository.findAllByMemberUserId(userId, tenantId).stream()
			.anyMatch(team -> !team.getId().equals(excludeTeamId));

		if (assignedElsewhere) {
			throw new CustomException(ErrorCode.TEAM_MEMBER_ALREADY_ASSIGNED);
		}
	}

	/**
	 * 같은 요청 안에서 사수와 부사수를 동일 사용자로 지정할 수 없다.
	 */
	public void requireDistinctMembers(Long mentorUserId, Long menteeUserId) {
		if (mentorUserId != null && mentorUserId.equals(menteeUserId)) {
			throw new CustomException(ErrorCode.TEAM_MEMBER_DUPLICATED);
		}
	}
}
