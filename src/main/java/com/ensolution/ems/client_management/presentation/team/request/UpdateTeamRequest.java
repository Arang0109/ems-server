package com.ensolution.ems.client_management.presentation.team.request;

public record UpdateTeamRequest(
	String name,
	Long mentorUserId,
	Long menteeUserId,
	String particleSamplerId,
	String gasSamplerId,
	String pitotTubeId,
	String nozzleId
) {}
