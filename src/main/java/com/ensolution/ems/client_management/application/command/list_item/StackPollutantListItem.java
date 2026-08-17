package com.ensolution.ems.client_management.application.command.list_item;

import com.ensolution.ems.global.common.enums.MeasurementCycle;

import java.math.BigDecimal;

/**
 * @param code 가이드 키. 모든 고객사에서 동일하므로 특정 물질 판별에 쓴다
 */
public record StackPollutantListItem(
	Long id,
	Long stackId,
	Long pollutantId,
	String code,
	String nameKr,
	String nameEn,
	MeasurementCycle cycle,
	BigDecimal allowance,
	boolean oxygenApplicable
) {}
