package com.ensolution.ems.client_management.presentation.target_substance;

public record TargetSubstanceResponse(
	Long id,
	Long preventionId,
	String name,
	Double removalEfficiency
) {
}
