package com.ensolution.ems.equipment.presentation.response;

import com.ensolution.ems.equipment.domain.InspectionType;

import java.time.LocalDate;

/**
 * 장비의 검사 종류별 설정과 최신 상태. 장비 응답에는 항상 전 종류가 내려간다.
 * {@code nextDueDate} 는 계산값이지만 record 접근자는 직렬화되지 않으므로 필드로 노출한다.
 */
public record InspectionItemResponse(
	InspectionType type,
	String typeLabel,
	boolean enabled,
	Integer cycleMonths,
	LocalDate lastInspectedAt,
	LocalDate nextDueDateOverride,
	LocalDate nextDueDate,
	boolean notificationEnabled
) {}
