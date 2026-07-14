package com.ensolution.ems.tenant.application.command.list_item;

import com.ensolution.ems.global.common.enums.MeasurementCycle;

import java.math.BigDecimal;

public record StackPollutantListItem(
	Long id,
	Long stackId,
	Long pollutantId,
	String nameKr,
	String nameEn,
	MeasurementCycle cycle,
	BigDecimal allowance
) {}
