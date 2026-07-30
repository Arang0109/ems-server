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
	 * {@code UserQueryUseCase}에 비예외 존재 확인 메서드가 없어 미존재를 예외로 판정한다.
	 * auth 모듈에 존재 확인용 조회가 추가되면 비예외 경로로 정리한다.
	 */
	public void requireMemberInTenant(Long userId, Long tenantId, ErrorCode notFound) {
		UserSummary user;
		try {
			user = userQueryUseCase.getUser(userId);
		} catch (CustomException e) {
			throw new CustomException(notFound);
		}
		if (!tenantId.equals(user.tenantId())) {
			throw new CustomException(notFound);
		}
	}
}
