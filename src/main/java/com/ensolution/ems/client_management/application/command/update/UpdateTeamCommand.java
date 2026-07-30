package com.ensolution.ems.client_management.application.command.update;

public record UpdateTeamCommand(
	String name,
	Long mentorUserId,
	Long menteeUserId,
	String particleSamplerId,
	String gasSamplerId,
	String pitotTubeId,
	String nozzleId
) {}
