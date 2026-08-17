package com.ensolution.ems.client_management.presentation.stack_pollutant.response;

import com.ensolution.ems.global.common.enums.MeasurementCycle;

/**
 * @param code 모든 고객사에서 동일한 전역 측정물질 키(예: {@code NOX}). 고객사 자체 물질이면 null
 */
public record StackPollutantListResponse(
	Long id,
	Long stackId,
	Long pollutantId,
	String code,
	String nameKr,
	String nameEn,
	MeasurementCycle cycle,
	String allowance,
	boolean oxygenApplicable
) {}
