package com.ensolution.ems.tenant.presentation.prevention.response;

public record PreventionResponse(
	Long id,
	Long stackId,
	String name,
	Double capacity
) {
}
