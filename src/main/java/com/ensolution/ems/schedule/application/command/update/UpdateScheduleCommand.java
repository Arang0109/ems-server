package com.ensolution.ems.schedule.application.command.update;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.schedule.domain.snapshot.TenantSnapshot;

import java.time.LocalDate;

/**
 * 측정계획 메타 수정 커맨드. 대상(stackId·teamId)은 변경 대상이 아니다.
 * 의뢰기관·사업장·측정시설 스냅샷 수정은 ChangeClientSnapshotCommand 경로를 사용한다.
 */
public record UpdateScheduleCommand(
	MeasurementField measurementField,
	LocalDate sampledAt,
	String schedulePurpose,
	String referenceNumber,
	TenantSnapshot tenant
) {}
