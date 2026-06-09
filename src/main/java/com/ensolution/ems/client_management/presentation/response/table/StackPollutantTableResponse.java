package com.ensolution.ems.client_management.presentation.response.table;

import com.ensolution.ems.global.common.enums.MeasurementCycle;

public record StackPollutantTableResponse(
	Long id,
	Long stackId,
	Long pollutantId,
	String nameKr,
	String nameEn,
	MeasurementCycle cycle,
	String allowance
) {}
