package com.ensolution.ems.client_management.presentation.measurement_method.response;

import com.ensolution.ems.global.common.enums.SampleGrouping;

import java.math.BigDecimal;

/**
 * @param mergedSampleName {@code MERGED}일 때만 값이 있다
 * @param samplingMinutes  표준 채취시간(분). 미지정이면 null
 * @param suctionFlowRate  표준 흡인유량(L/min). 미지정이면 null
 */
public record MeasurementMethodResponse(
	Long id,
	String name,
	SampleGrouping sampleGrouping,
	String mergedSampleName,
	Integer samplingMinutes,
	BigDecimal suctionFlowRate,
	Integer sortOrder
) {}
