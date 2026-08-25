package com.ensolution.ems.equipment.application.command;

import com.ensolution.ems.equipment.domain.InspectionResult;
import com.ensolution.ems.equipment.domain.InspectionType;

import java.time.LocalDate;

public record RecordInspectionCommand(
	String equipmentId,
	Long tenantId,
	InspectionType type,
	LocalDate inspectedAt,
	/** 성적서에 유효기간이 명시된 경우의 만료일. 있으면 주기 계산 대신 이 날짜가 다음 예정일이 된다. */
	LocalDate validUntil,
	String agency,
	String certificateNumber,
	InspectionResult result,
	String remark
) {
}
