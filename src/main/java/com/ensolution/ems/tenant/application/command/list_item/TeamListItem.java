package com.ensolution.ems.tenant.application.command.list_item;

public record TeamListItem(
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
