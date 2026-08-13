package com.ensolution.ems.client_management.application.port.in;

/**
 * 타 모듈(auth 로그인 등)이 "이 사용자가 속한 팀"만 필요할 때 사용하는 경량 팀 요약 VO.
 * 사수·부사수 이름 조립(auth 왕복)이 필요한 {@link TeamSummary}와 달리 팀 식별 정보만 담는다.
 */
public record UserTeamSummary(
	Long teamId,
	String teamName
) {}
