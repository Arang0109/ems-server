package com.ensolution.ems.schedule.presentation.request;

import com.ensolution.ems.global.common.enums.MeasurementField;

import java.time.LocalDateTime;

/** 측정계획 메타 수정 요청. 전달하지 않은 필드는 기존 값을 유지한다. */
public record UpdateScheduleRequest(
	MeasurementField measurementField,
	LocalDateTime measureDate,
	String measurementType,
	String referenceNumber
) {}
