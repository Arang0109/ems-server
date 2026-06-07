package com.ensolution.ems.client_management.presentation.response;

public record StackPollutantResponse(
	Long id,
	Long stackId,
	Long pollutantId,
	String nameKr,
	String nameEn
) {}
