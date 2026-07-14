package com.ensolution.ems.schedule.presentation.response;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.schedule.domain.ScheduleStatus;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;

import java.time.LocalDateTime;

/** 측정계획 상세 응답. 메타데이터와 측정 시점 세부 스냅샷 트리를 함께 노출한다. */
public record ScheduleResponse(
	Long id,
	Long tenantId,
	Long stackId,
	Long teamId,
	MeasurementField measurementField,
	LocalDateTime measureDate,
	String measurementType,
	ScheduleStatus status,
	String referenceNumber,
	LocalDateTime createdAt,
	LocalDateTime modifiedAt,
	ScheduleSnapshot snapshot
) {}
