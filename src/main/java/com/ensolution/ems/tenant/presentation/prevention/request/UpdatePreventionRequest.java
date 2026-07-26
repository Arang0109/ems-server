package com.ensolution.ems.tenant.presentation.prevention.request;

public record UpdatePreventionRequest(
	String name,
	Double capacity
) {
}
