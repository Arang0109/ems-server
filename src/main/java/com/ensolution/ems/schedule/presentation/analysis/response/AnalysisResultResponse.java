package com.ensolution.ems.schedule.presentation.analysis.response;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 실험분석정보 응답. 측정항목 하나에 대응하며 판정 근거와 분석 결과를 함께 담는다.
 *
 * <p>허용기준치·산소보정 적용 여부는 측정 시점 원장(stack_pollutant) 사본이며 이 경로의 수정
 * 대상이 아니다 — 초과 판정의 근거를 회차에 고정하기 위해서다.
 * (정정이 필요하면 {@code PATCH /api/schedules/{scheduleId}/items/{pollutantId}}를 쓴다.)
 *
 * <p>분석 결과는 측정계획 문서의 측정항목 안에 저장되므로 문서 대리키가 없다.
 * <b>식별은 측정물질({@code pollutantId})로 한다.</b>
 *
 * <p>채취시간은 성적서 탭이, 분석값 4종은 실험·분석 탭이 소유하며 저장 경로가 갈라져 있다
 * ({@code PUT .../analyses/sampling-times}, {@code PUT .../analyses/results}).
 */
public record AnalysisResultResponse(
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
	LocalTime samplingEndedAt
) {}
