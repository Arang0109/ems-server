package com.ensolution.ems.client_management.application.command;

public record StackPollutantListItem(
	Long id,
	Long stackId,
	Long pollutantId,
	String nameKr,
	String nameEn
) {}
