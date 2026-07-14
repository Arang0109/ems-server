package com.ensolution.ems.tenant.presentation.target_substance.request;

import jakarta.validation.constraints.NotNull;

public record CreateTargetSubstanceRequest(
	@NotNull
	Long preventionId,
	String name,
	Double removalEfficiency
) {
}
