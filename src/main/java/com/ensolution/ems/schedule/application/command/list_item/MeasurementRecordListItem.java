package com.ensolution.ems.schedule.application.command.list_item;

import com.ensolution.ems.global.common.enums.MeasurementCycle;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 측정지점 이력 목록의 한 줄 = "어느 회차에 어느 항목을 측정했고 값이 얼마였는가".
 * 측정일 내림차순으로 반환하므로 같은 {@code scheduleId}가 연달아 나오며, 화면은 회차 단위로 묶어 보여준다.
 *
 * @param periodKey 이행한 주기 구간. 주기가 지정되지 않은 항목은 null이다
 */
public record MeasurementRecordListItem(
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
