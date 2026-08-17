package com.ensolution.ems.client_management.presentation.pollutant.response;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;

/**
 * 측정물질 단건.
 *
 * <p>{@code nameKr}·{@code nameEn}·{@code equipment}·{@code testMethod}는 고객사가 관리하는 값이고,
 * {@code code}·{@code field}·{@code method}·{@code phase}는 가이드(카탈로그)에서 채워지는 값이다.
 */
public record PollutantResponse(
	Long id,
	Long catalogId,
	String code,
	MeasurementField field,
	String nameKr,
	String nameEn,
	MeasurementMethod method,
	PollutantPhase phase,
	String equipment,
	String testMethod
) {}
