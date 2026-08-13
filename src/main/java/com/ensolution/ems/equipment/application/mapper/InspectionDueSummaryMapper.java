package com.ensolution.ems.equipment.application.mapper;

import com.ensolution.ems.equipment.application.port.in.InspectionDueSummary;
import com.ensolution.ems.equipment.domain.Equipment;
import com.ensolution.ems.equipment.domain.InspectionItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 인터모듈 매퍼: 장비와 그 검사 항목을 타 모듈 공개용 {@code port/in} 요약 VO로 변환한다.
 * <p>
 * 장비 1건이 검사 항목 N건으로 펼쳐지는 변환이라 목록 메서드를 두지 않는다.
 * 리프 변환만 여기서 담당하고 평탄화·정렬은 서비스가 한다.
 */
@Mapper(componentModel = "spring")
public interface InspectionDueSummaryMapper {

	@Mapping(target = "equipmentId", source = "equipment.id")
	@Mapping(target = "equipmentName", source = "equipment.equipmentName")
	@Mapping(target = "managementNumber", source = "equipment.managementNumber")
	@Mapping(target = "inspectionType", source = "item.type")
	@Mapping(target = "nextDueDate", expression = "java(item.nextDueDate())")
	InspectionDueSummary toInspectionDueSummary(Equipment equipment, InspectionItem item);
}
