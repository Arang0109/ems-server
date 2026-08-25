package com.ensolution.ems.equipment.presentation.request;

import com.ensolution.ems.equipment.domain.InspectionResult;
import com.ensolution.ems.equipment.domain.InspectionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record RecordInspectionRequest(
	@NotNull(message = "검사 종류는 필수 선택값입니다.")
	InspectionType type,

	@NotNull(message = "검사 실시일은 필수 입력값입니다.")
	@PastOrPresent LocalDate inspectedAt,

	/* 성적서에 명시된 유효기간 만료일. 전달하면 다음 검사 예정일이 이 날짜로 지정된다. */
	LocalDate validUntil,

	String agency,
	String certificateNumber,
	InspectionResult result,
	String remark
) {}
