package com.ensolution.ems.client_management.application.command.detail;

public record TeamDetail(
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
