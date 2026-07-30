package com.ensolution.ems.client_management.presentation.team.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTeamRequest(
	@NotBlank
	String name,
	@NotNull
	Long mentorUserId,
	@NotNull
	Long menteeUserId,
	@NotBlank
	String particleSamplerId,
	@NotBlank
	String gasSamplerId,
	@NotBlank
	String pitotTubeId,
	@NotBlank
	String nozzleId
) {}
