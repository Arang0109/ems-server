package com.ensolution.ems.client_management.application.command.create;

import com.ensolution.ems.global.common.enums.MeasurementCycle;

import java.math.BigDecimal;

/**
 * @param pollutantId 등록 대상 측정물질 id. null이면 {@code catalogId}로 해석한다
 * @param catalogId   전역 카탈로그 id. 이 고객사의 측정물질이 없으면 등록 시점에 만들어진다
 */
public record CreateStackPollutantCommand(
	Long tenantId,
	Long stackId,
	Long pollutantId,
	Long catalogId,
	MeasurementCycle cycle,
	BigDecimal allowance,
	boolean oxygenApplicable
) {
	/** 측정물질 참조가 해석된 커맨드를 만든다. */
	public CreateStackPollutantCommand withPollutantId(Long resolvedPollutantId) {
		return new CreateStackPollutantCommand(
			tenantId, stackId, resolvedPollutantId, catalogId, cycle, allowance, oxygenApplicable);
	}
}
