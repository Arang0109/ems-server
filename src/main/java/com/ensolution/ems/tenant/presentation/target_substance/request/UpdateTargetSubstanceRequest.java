package com.ensolution.ems.tenant.presentation.target_substance.request;

public record UpdateTargetSubstanceRequest(
	String name,
	Double removalEfficiency
) {
}
