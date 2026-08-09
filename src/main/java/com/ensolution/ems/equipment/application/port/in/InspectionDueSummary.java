package com.ensolution.ems.equipment.application.port.in;

import com.ensolution.ems.equipment.domain.InspectionType;

import java.time.LocalDate;

/**
 * 검사 예정일이 임박한 <b>장비-검사항목</b> 단위 요약 VO.
 * <p>
 * 한 장비가 여러 검사에서 임박하면 항목 수만큼 여러 건이 나온다.
 * 잔여일수 같은 표시용 파생값은 담지 않는다. 기준일을 아는 소비 모듈이 계산한다.
 */
public record InspectionDueSummary(
	String equipmentId,
	String equipmentName,
	String managementNumber,
	InspectionType inspectionType,
	LocalDate nextDueDate
) {}
