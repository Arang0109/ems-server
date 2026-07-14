package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.global.common.enums.MeasurementField;

import java.time.LocalDateTime;

/** 측정계획 기본 정보 스냅샷. 메타데이터의 사본으로, 문서 단독 조회 시 표시에 사용한다. */
public record BasicInfo(
	String referenceNumber,
	LocalDateTime measureDate,
	MeasurementField measurementField,
	String measurementType
) {}
