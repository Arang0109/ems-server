package com.ensolution.ems.schedule.presentation.request;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.schedule.domain.snapshot.ClientSnapshot;

import java.time.LocalDateTime;

/**
 * 측정계획 메타 수정 요청. 전달하지 않은 필드는 기존 값을 유지한다.
 * {@code client}를 전달하면 시설·의뢰기관 정보(문서 스냅샷 트리)를 전체 교체하며, null이면 미변경이다.
 */
public record UpdateScheduleRequest(
	MeasurementField measurementField,
	LocalDateTime measureDate,
	String measurementType,
	String referenceNumber,
	ClientSnapshot client
) {}
