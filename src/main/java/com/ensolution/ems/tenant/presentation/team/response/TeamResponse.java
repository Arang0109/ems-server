package com.ensolution.ems.tenant.presentation.team.response;

public record TeamResponse(
	Long id,
	String name,
	Long mentorUserId,
	String mentorName,
	Long menteeUserId,
	String menteeName,
	String particleSamplerId,
	String gasSamplerId,
	String pitotTubeId,
	String nozzleId
) {}
