package com.ensolution.ems.tenant.application.port.in;

/**
 * 타 모듈(schedule 등)이 측정 시점 팀 스냅샷을 구성할 때 사용하는 팀 요약 VO.
 * 사수·부사수의 이름은 auth 모듈에서 조립되며, 장비 id 4종은 값 그대로 노출한다.
 */
public record TeamSummary(
	Long teamId,
	String teamName,
	Long mentorUserId,
	String mentorName,
	Long menteeUserId,
	String menteeName,
	String particleSamplerId,
	String gasSamplerId,
	String pitotTubeId,
	String nozzleId
) {}
