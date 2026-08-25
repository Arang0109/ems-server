package com.ensolution.ems.dashboard.application.mapper;

import com.ensolution.ems.dashboard.application.command.InspectionDue;
import com.ensolution.ems.equipment.application.port.in.InspectionDueSummary;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 인터모듈 매퍼: 공급 모듈(equipment)의 {@code port/in} 요약 VO를 대시보드 표시용 VO로 변환한다.
 * 잔여일수는 대시보드가 기준일(baseDate)을 정해 계산하는 파생값이며, 검사 기한이 지난 항목은 음수가 된다.
 */
@Mapper(componentModel = "spring", imports = ChronoUnit.class)
public interface EquipmentPortMapper {

	@Mapping(target = "inspectionTypeLabel",
		expression = "java(summary.inspectionType() == null ? null : summary.inspectionType().getDesc())")
	@Mapping(target = "daysRemaining",
		expression = "java(ChronoUnit.DAYS.between(baseDate, summary.nextDueDate()))")
	InspectionDue toInspectionDue(InspectionDueSummary summary, @Context LocalDate baseDate);

	List<InspectionDue> toInspectionDues(List<InspectionDueSummary> summaries, @Context LocalDate baseDate);
}
