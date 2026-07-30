package com.ensolution.ems.equipment.application.mapper;

import com.ensolution.ems.equipment.application.port.in.CalibrationDueSummary;
import com.ensolution.ems.equipment.domain.Equipment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 인터모듈 매퍼: 장비 도메인을 타 모듈 공개용 {@code port/in} 교정 요약 VO로 변환한다.
 */
@Mapper(componentModel = "spring")
public interface CalibrationDueSummaryMapper {

	@Mapping(target = "equipmentId", source = "id")
	@Mapping(target = "nextCalibrationDate", expression = "java(equipment.nextCalibrationDate())")
	CalibrationDueSummary toCalibrationDueSummary(Equipment equipment);

	List<CalibrationDueSummary> toCalibrationDueSummaries(List<Equipment> equipments);
}
