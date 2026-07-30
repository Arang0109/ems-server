package com.ensolution.ems.client_management.application.command.create;

public record CreateTeamCommand(
	Long tenantId,
	String name,
	Long mentorUserId,
	Long menteeUserId,
	String particleSamplerId,
	String gasSamplerId,
	String pitotTubeId,
	String nozzleId
) {}
