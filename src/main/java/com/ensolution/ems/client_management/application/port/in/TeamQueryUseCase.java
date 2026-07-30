package com.ensolution.ems.client_management.application.port.in;

/**
 * 측정 팀 정보를 타 모듈에 제공하는 인바운드 포트.
 * 소유권 격리를 위해 tenantId를 함께 받으며, 미존재·타 tenant는 NOT_FOUND로 은닉한다.
 */
public interface TeamQueryUseCase {
	TeamSummary getTeamSummary(Long teamId, Long tenantId);
}
