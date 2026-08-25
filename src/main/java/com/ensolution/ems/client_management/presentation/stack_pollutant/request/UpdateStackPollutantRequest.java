package com.ensolution.ems.client_management.presentation.stack_pollutant.request;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 시설별 측정물질의 측정 조건 수정 요청.
 *
 * <p>{@code stackId}·{@code pollutantId}는 받지 않는다 — 어떤 시설의 어떤 물질인지가 바뀌면
 * 다른 항목이므로 삭제 후 재등록이다. {@code allowance}는 null이면 "미지정"으로 비운다.
 */
public record UpdateStackPollutantRequest(
	@NotNull
	MeasurementCycle cycle,
	BigDecimal allowance,
	boolean oxygenApplicable
) {}
