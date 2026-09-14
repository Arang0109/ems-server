package com.ensolution.ems.client_management.presentation.measurement_method.request;

import com.ensolution.ems.global.common.enums.SampleGrouping;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * @param sampleGrouping   채취 단위. {@code MERGED}면 {@code mergedSampleName}이 필수이고, 그 외에는 비워야 합니다
 * @param mergedSampleName 한 병으로 함께 채취할 때 기록지에 적는 통칭명(예: {@code VOCs})
 * @param samplingMinutes  표준 채취시간(분). 회차별 실측 시각이 아니라 계획 기본값입니다
 * @param suctionFlowRate  표준 흡인유량(L/min). 통칭 시료(VOCs·VOCs-T)는 이 값이 곧 그 병의 유량이고, 항목별 채취 방법은 측정물질이 덮어쓸 수 있습니다
 * @param sortOrder        비우면 목록 맨 뒤에 붙습니다
 */
public record CreateMeasurementMethodRequest(
	@NotBlank
	String name,
	@NotNull
	SampleGrouping sampleGrouping,
	String mergedSampleName,
	@PositiveOrZero
	Integer samplingMinutes,
	@PositiveOrZero
	BigDecimal suctionFlowRate,
	Integer sortOrder
) {}
