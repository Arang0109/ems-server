package com.ensolution.ems.schedule.presentation.response.snapshot;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 측정항목의 실험실 분석 결과 응답. <b>null이면 아직 분석 전</b>이며 정상 상태다.
 *
 * <p>앞의 넷은 실험·분석 탭이, 뒤의 둘은 성적서 탭이 소유하며 저장 경로가 갈라져 있다
 * ({@code PUT .../analyses/results}, {@code PUT .../analyses/sampling-times}).
 */
public record AnalysisResultResponse(
	BigDecimal analysisValue,
	String unit,
	String analysisMethod,
	String analysisEquipment,
	LocalTime samplingStartedAt,
	LocalTime samplingEndedAt
) {}
