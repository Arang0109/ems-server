package com.ensolution.ems.client_management.presentation.stack_pollutant;

import com.ensolution.ems.global.common.enums.MeasurementCycle;

public record StackPollutantResponse(
	Long id,
	Long stackId,
	Long pollutantId,
	MeasurementCycle cycle,
	String allowance
) {}
