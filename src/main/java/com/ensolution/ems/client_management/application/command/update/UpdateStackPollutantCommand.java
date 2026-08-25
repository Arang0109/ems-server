package com.ensolution.ems.client_management.application.command.update;

import com.ensolution.ems.global.common.enums.MeasurementCycle;

import java.math.BigDecimal;

/**
 * 시설별 측정물질의 측정 조건 수정. 어떤 시설의 어떤 물질인지({@code stackId}·{@code pollutantId})는
 * 대상이 아니다 — 그것이 바뀌면 다른 항목이지 수정이 아니다.
 */
public record UpdateStackPollutantCommand(
	MeasurementCycle cycle,
	BigDecimal allowance,
	boolean oxygenApplicable
) {}
