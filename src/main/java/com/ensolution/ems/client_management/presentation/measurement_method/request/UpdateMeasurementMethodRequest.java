package com.ensolution.ems.client_management.presentation.measurement_method.request;

import com.ensolution.ems.global.common.enums.SampleGrouping;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * {@code name}·{@code sampleGrouping}은 비우면 기존 값이 유지됩니다.
 * {@code mergedSampleName}·{@code samplingMinutes}는 <b>보낸 값이 그대로 저장</b>됩니다 — 비우면 지워집니다.
 * 통칭 채취를 항목별 채취로 바꾸려면 {@code sampleGrouping=PER_ITEM}과 빈 {@code mergedSampleName}을 함께 보내세요.
 */
public record UpdateMeasurementMethodRequest(
	String name,
	SampleGrouping sampleGrouping,
	String mergedSampleName,
	@PositiveOrZero
	Integer samplingMinutes
) {}
