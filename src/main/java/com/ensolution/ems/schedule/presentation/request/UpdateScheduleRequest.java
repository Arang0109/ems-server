package com.ensolution.ems.schedule.presentation.request;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.schedule.domain.snapshot.TenantSnapshot;

import java.time.LocalDate;

/**
 * 측정계획 메타 수정 요청. 전달하지 않은 필드는 기존 값을 유지한다.
 * 의뢰기관·사업장·측정시설 스냅샷 수정은 {@code PATCH /api/schedules/{scheduleId}/client}를 사용한다.
 */
public record UpdateScheduleRequest(
	MeasurementField measurementField,
	LocalDate sampledAt,
	String schedulePurpose,
	String referenceNumber,
	TenantSnapshot tenant
) {}
