package com.ensolution.ems.tenant.presentation.target_substance.response;

public record TargetSubstanceResponse(
	Long id,
	Long preventionId,
	String name,
	Double removalEfficiency
) {
}
