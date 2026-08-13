package com.ensolution.ems.client_management.presentation.stack_pollutant.response;

import com.ensolution.ems.global.common.enums.MeasurementCycle;

public record StackPollutantListResponse(
	Long id,
	Long stackId,
	Long pollutantId,
	String nameKr,
	String nameEn,
	MeasurementCycle cycle,
	String allowance,
	boolean oxygenApplicable
) {}
