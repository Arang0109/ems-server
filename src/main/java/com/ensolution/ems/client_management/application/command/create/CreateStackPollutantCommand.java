package com.ensolution.ems.client_management.application.command.create;

import com.ensolution.ems.global.common.enums.MeasurementCycle;

import java.math.BigDecimal;

/**
 * @param pollutantId 등록 대상 측정물질 id. 이 고객사가 이미 채택한 물질이어야 한다
 */
public record CreateStackPollutantCommand(
	Long tenantId,
	Long stackId,
	Long pollutantId,
	MeasurementCycle cycle,
	BigDecimal allowance,
	boolean oxygenApplicable
) {}
