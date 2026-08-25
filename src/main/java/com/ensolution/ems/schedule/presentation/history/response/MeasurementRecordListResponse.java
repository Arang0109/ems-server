package com.ensolution.ems.schedule.presentation.history.response;

import com.ensolution.ems.global.common.enums.MeasurementCycle;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 측정지점 이력 목록 응답. 측정일 내림차순이며 같은 {@code scheduleId}가 연달아 나오므로,
 * 화면은 회차 단위로 묶어 표시한다.
 */
public record MeasurementRecordListResponse(
	Long recordId,
	Long scheduleId,
	LocalDate sampledAt,
	Long pollutantId,
	String code,
	String nameKr,
	MeasurementCycle cycle,
	String periodKey,
	String periodLabel,
	BigDecimal concentration,
	String unit,
	BigDecimal correctedConcentration,
	BigDecimal emission,
	BigDecimal allowance,
	Boolean exceeded
) {}
