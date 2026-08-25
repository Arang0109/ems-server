package com.ensolution.ems.schedule.application.command.create;

import java.math.BigDecimal;

/**
 * 실험분석정보 등록 파라미터.
 * 허용기준치·산소보정 적용 여부는 측정계획 스냅샷의 측정항목에서 복사하므로 여기 담지 않는다.
 */
public record CreateAnalysisRecordCommand(
	Long pollutantId,
	BigDecimal analysisValue,
	String unit,
	String analysisMethod,
	String analysisEquipment
) {}
