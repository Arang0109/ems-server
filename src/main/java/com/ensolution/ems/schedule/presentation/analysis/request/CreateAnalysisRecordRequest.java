package com.ensolution.ems.schedule.presentation.analysis.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 실험분석정보 등록 요청.
 * <p>
 * 측정분석값은 필수가 아니다 — 성적서 탭이 채취시간만 먼저 저장해 둔 기록이 정상 상태이므로,
 * 값이 아직 없는 기록을 막을 이유가 없다. 채취시간은 이 경로로 받지 않는다(성적서 탭 소유).
 */
public record CreateAnalysisRecordRequest(
	@NotNull(message = "측정물질은 필수 값입니다.")
	Long pollutantId,
	BigDecimal analysisValue,
	String unit,
	String analysisMethod,
	String analysisEquipment
) {}
