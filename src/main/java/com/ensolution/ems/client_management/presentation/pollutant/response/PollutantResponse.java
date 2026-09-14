package com.ensolution.ems.client_management.presentation.pollutant.response;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMode;
import com.ensolution.ems.global.common.enums.PollutantPhase;
import com.ensolution.ems.global.common.enums.SampleGrouping;

import java.math.BigDecimal;

/**
 * 측정물질 단건.
 *
 * <p>{@code methodId}·{@code samplingMinutes}·{@code nameKr}·{@code nameEn}·{@code equipment}·{@code testMethod}는
 * 고객사가 관리하는 값이고, {@code code}·{@code field}·{@code phase}는 가이드(카탈로그)에서, {@code methodName}·
 * {@code sampleGrouping}·{@code mergedSampleName}·{@code methodSamplingMinutes}·{@code methodSuctionFlowRate}는 측정방법에서 채워지는 값이다.
 * 백필되지 못한 레거시 행은 측정방법 값이 전부 null이다.
 *
 * @param samplingMinutes          항목별 채취시간 오버라이드(분). 없으면 null
 * @param methodSamplingMinutes    측정방법의 표준 채취시간(분)
 * @param effectiveSamplingMinutes 이 항목에 실제 적용되는 채취시간 — 화면은 이 값을 보여 준다
 * @param suctionFlowRate          항목별 흡인유량 오버라이드(L/min). 없으면 null
 * @param methodSuctionFlowRate    측정방법의 표준 흡인유량(L/min)
 * @param effectiveSuctionFlowRate 이 항목에 실제 적용되는 흡인유량
 */
public record PollutantResponse(
	Long id,
	Long catalogId,
	String code,
	MeasurementField field,
	String nameKr,
	String nameEn,
	Long methodId,
	String methodName,
	SampleGrouping sampleGrouping,
	String mergedSampleName,
	Integer samplingMinutes,
	Integer methodSamplingMinutes,
	Integer effectiveSamplingMinutes,
	BigDecimal suctionFlowRate,
	BigDecimal methodSuctionFlowRate,
	BigDecimal effectiveSuctionFlowRate,
	PollutantPhase phase,
	/** 측정방식 분류 — 카탈로그 투영값. 회사가 측정방법을 쪼개도 이 축으로 묶인다 */
	MeasurementMode mode,
	String equipment,
	String testMethod
) {}
