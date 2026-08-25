package com.ensolution.ems.schedule.application.command.update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 항목별 실험분석 결과 일괄 저장 파라미터.
 * 전달한 항목만 갱신하며, 전달한 항목의 null 값은 기존 값을 비운다(미전달과 구분된다).
 */
public record SaveAnalysisResultsCommand(List<Entry> items) {

	public record Entry(
		Long pollutantId,
		BigDecimal analysisValue,
		String unit,
		String analysisMethod,
		String analysisEquipment
	) {}
}
