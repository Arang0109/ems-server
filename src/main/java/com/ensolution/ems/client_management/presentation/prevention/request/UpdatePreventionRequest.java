package com.ensolution.ems.client_management.presentation.prevention.request;

public record UpdatePreventionRequest(
	String name,
	Double capacity,
	String targetName,
	String removalEfficiency
) {
}
