package com.ensolution.ems.client_management.application.command.update;

import com.ensolution.ems.global.common.enums.MeasurementMethod;

/**
 * 고객사 소유값만 수정한다.
 *
 * <p>{@code catalogId}는 대상이 아니다 — 어떤 물질인지가 바뀌면 다른 물질이지 수정이 아니다.
 * 측정분야·형태도 카탈로그가 소유하므로 여기서 바꿀 수 없다.
 * 측정방법은 고객사 소유값이므로 바꿀 수 있다 — null이면 기존 값을 유지한다.
 */
public record UpdatePollutantCommand(
	MeasurementMethod method,
	String nameKr,
	String nameEn,
	String equipment,
	String testMethod
) {}
