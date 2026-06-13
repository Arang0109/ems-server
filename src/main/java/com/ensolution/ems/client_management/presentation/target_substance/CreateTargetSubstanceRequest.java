package com.ensolution.ems.client_management.presentation.target_substance;

import jakarta.validation.constraints.NotNull;

public record CreateTargetSubstanceRequest(
	@NotNull
	Long preventionId,
	String name,
	Double removalEfficiency
) {
}
