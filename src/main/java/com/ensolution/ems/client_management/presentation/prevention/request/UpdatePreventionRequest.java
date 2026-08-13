package com.ensolution.ems.client_management.presentation.prevention.request;

public record UpdatePreventionRequest(
	String name,
	Double capacity,
	String unit,
	String targetName,
	String removalEfficiency
) {
}
