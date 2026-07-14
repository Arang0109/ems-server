package com.ensolution.ems.schedule.domain.snapshot;

/** 측정 시점 팀 스냅샷. */
public record TeamSnapshot(
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
