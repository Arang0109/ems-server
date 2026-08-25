package com.ensolution.ems.schedule.presentation.response;

import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;

import java.time.LocalDate;

/**
 * 이전 회차 기록지 응답. 불러올 기록이 없으면 {@code data}가 null로 내려간다 —
 * 첫 회차이거나 그 기록지를 처음 쓰는 경우이며 오류가 아니다.
 */
public record PreviousSheetResponse(
	Long sourceScheduleId,
	LocalDate sampledAt,
	String referenceNumber,
	MeasurementSheet sheet
) {}
