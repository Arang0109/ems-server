package com.ensolution.ems.client_management.application.port.in;

/**
 * 측정 팀 정보를 타 모듈에 제공하는 인바운드 포트.
 * 소유권 격리를 위해 tenantId를 함께 받으며, 미존재·타 tenant는 NOT_FOUND로 은닉한다.
 */
public interface TeamQueryUseCase {
	TeamSummary getTeamSummary(Long teamId, Long tenantId);

	/**
	 * 사용자가 사수 또는 부사수로 배정된 팀을 조회한다.
	 * 팀 미배정은 정상 상태이므로 예외를 던지지 않고 {@code null}을 반환한다.
	 */
	UserTeamSummary getUserTeamSummary(Long userId, Long tenantId);
}
