package com.ensolution.ems.schedule.presentation.response.snapshot;

import com.ensolution.ems.global.common.enums.MeasurementField;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 측정계획 기본 정보 응답. 성적서 머리말에 그대로 실리는 블록이다.
 * <p>
 * {@code referenceNumber}·{@code sampledAt}·{@code measurementField}·{@code schedulePurpose}는
 * 응답 최상위의 메타 필드와 값이 같다. 중복이지만 역할이 다르므로 함께 내보낸다 —
 * 최상위는 계획을 식별·검색하는 축이고, 여기 있는 것은 성적서 기본정보 표의 칸이다.
 * 두 값은 서버가 {@code BasicInfo#applyMeta}로 동기화하며, 어긋날 경우 진실은 최상위(메타)다.
 */
public record BasicInfoResponse(
	String referenceNumber,

	String facilityManager,
	String samplingWitness,
	String analyst,
	String technicalManager,

	LocalDate sampledAt,
	LocalDate receivedAt,
	LocalDate analyzedAt,
	LocalDate issuedAt,

	LocalTime samplingStartedAt,
	LocalTime samplingEndedAt,

	MeasurementField measurementField,
	String schedulePurpose
) {}
