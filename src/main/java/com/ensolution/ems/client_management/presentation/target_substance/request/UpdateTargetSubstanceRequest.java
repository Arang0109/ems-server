package com.ensolution.ems.client_management.presentation.target_substance.request;

public record UpdateTargetSubstanceRequest(
	String name,
	Double removalEfficiency
) {
}
