package com.ensolution.ems.tenant.presentation.stack_pollutant.response;

import com.ensolution.ems.global.common.enums.MeasurementCycle;

public record StackPollutantResponse(
	Long id,
	Long stackId,
	Long pollutantId,
	MeasurementCycle cycle,
	String allowance
) {}
