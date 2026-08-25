package com.ensolution.ems.client_management.presentation.stack_pollutant.request;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * @param pollutantId 이 고객사가 채택해 보유 중인 측정물질 id.
 *                    아직 채택하지 않은 가이드 항목은 먼저 {@code POST /api/pollutants}로 채택해야 한다
 */
public record CreateStackPollutantRequest(
	@NotNull
	Long stackId,
	@NotNull
	Long pollutantId,
	@NotNull
	MeasurementCycle cycle,
	BigDecimal allowance,
	boolean oxygenApplicable
) {}
