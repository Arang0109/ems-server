package com.ensolution.ems.client_management.application.command.create;

import com.ensolution.ems.global.common.enums.SampleGrouping;

import java.math.BigDecimal;

/**
 * @param mergedSampleName {@code MERGED}일 때의 통칭 시료명. 그 외 채취 단위에서는 null이어야 한다
 * @param samplingMinutes  표준 채취시간(분). 없으면 null
 * @param suctionFlowRate  표준 흡인유량(L/min). 없으면 null
 * @param sortOrder        없으면 서비스가 목록 맨 뒤({@code max+10})를 부여한다
 */
public record CreateMeasurementMethodCommand(
	Long tenantId,
	String name,
	SampleGrouping sampleGrouping,
	String mergedSampleName,
	Integer samplingMinutes,
	BigDecimal suctionFlowRate,
	Integer sortOrder
) {}
