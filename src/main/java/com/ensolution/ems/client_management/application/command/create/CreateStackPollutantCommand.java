package com.ensolution.ems.client_management.application.command.create;

import com.ensolution.ems.global.common.enums.MeasurementCycle;

import java.math.BigDecimal;

public record CreateStackPollutantCommand(
	Long tenantId,
	Long stackId,
	Long pollutantId,
	MeasurementCycle cycle,
	BigDecimal allowance
) {}