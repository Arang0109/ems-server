package com.ensolution.ems.schedule.application.command.update;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.schedule.domain.snapshot.ClientSnapshot;

import java.time.LocalDateTime;

/**
 * 측정계획 메타 수정 커맨드. 대상(stackId·teamId)은 변경 대상이 아니다.
 * {@code client}는 문서 스냅샷의 시설·의뢰기관 트리 전체 교체용이며, null이면 미변경이다(원장 tenant는 변경하지 않는다).
 */
public record UpdateScheduleCommand(
	MeasurementField measurementField,
	LocalDateTime measureDate,
	String measurementType,
	String referenceNumber,
	ClientSnapshot client
) {}
