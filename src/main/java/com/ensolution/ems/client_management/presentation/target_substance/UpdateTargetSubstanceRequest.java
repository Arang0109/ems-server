package com.ensolution.ems.client_management.presentation.target_substance;

public record UpdateTargetSubstanceRequest(
	String name,
	Double removalEfficiency
) {
}
