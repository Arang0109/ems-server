package com.ensolution.ems.schedule.presentation.response.snapshot;

/** 측정 시점 팀 스냅샷 응답. */
public record TeamSnapshotResponse(
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
