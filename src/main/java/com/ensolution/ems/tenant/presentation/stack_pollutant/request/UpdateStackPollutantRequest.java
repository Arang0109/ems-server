package com.ensolution.ems.tenant.presentation.stack_pollutant.request;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateStackPollutantRequest(
	@NotNull
	Long stackId,
	@NotNull
	Long pollutantId,
	@NotNull
	MeasurementCycle cycle,
	BigDecimal allowance
) {}
