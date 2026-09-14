package com.ensolution.ems.schedule.presentation.response.snapshot;

import com.ensolution.ems.global.common.enums.SampleGrouping;

import java.math.BigDecimal;

/**
 * 측정 시점 측정방법 사본 응답. 현장 기록지가 가스상 시료 행을 만드는 근거다.
 *
 * @param methodId         원장 연결키. 구 문서(마이그레이션으로 변환)는 null이므로 {@code methodId ?? name}으로 그룹을 식별한다
 * @param mergedSampleName {@code MERGED}일 때만 값이 있다
 * @param samplingMinutes  표준 채취시간(분). 계획 기본값이며 실측 시각과 다르다
 * @param suctionFlowRate  표준 흡인유량(L/min). 통칭 시료의 유량은 이 값이 정한다. 도입 이전 문서는 null
 */
public record MeasurementMethodSnapshotResponse(
	Long methodId,
	String name,
	SampleGrouping sampleGrouping,
	String mergedSampleName,
	Integer samplingMinutes,
	BigDecimal suctionFlowRate
) {}
