package com.ensolution.ems.client_management.presentation.request;

import jakarta.validation.constraints.NotNull;

public record AssignStackPollutantRequest(
	@NotNull
	Long stackId,
	@NotNull
	Long pollutantId
) {}
