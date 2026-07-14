package com.ensolution.ems.tenant.application.command.create;

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
