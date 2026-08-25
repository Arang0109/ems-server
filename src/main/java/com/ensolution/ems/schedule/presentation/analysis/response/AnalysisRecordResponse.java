package com.ensolution.ems.schedule.presentation.analysis.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 실험분석정보 응답. 허용기준치·산소보정 적용 여부는 측정 시점 원장(stack_pollutant) 사본이며
 * 수정 대상이 아니다 — 초과 판정의 근거를 회차에 고정하기 위해서다.
 * <p>
 * 채취시간은 성적서 탭이 소유하는 필드이며 별도 경로
 * ({@code PUT /api/schedules/{scheduleId}/analyses/sampling-times})로 저장한다.
 */
public record AnalysisRecordResponse(
	String id,
	Long scheduleId,
	Long stackPollutantId,
	Long pollutantId,
	String pollutantName,
	BigDecimal allowance,
	boolean oxygenApplicable,
	BigDecimal analysisValue,
	String unit,
	String analysisMethod,
	String analysisEquipment,
	LocalTime samplingStartedAt,
	LocalTime samplingEndedAt,
	LocalDateTime createdAt,
	LocalDateTime modifiedAt
) {}
