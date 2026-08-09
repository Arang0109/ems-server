package com.ensolution.ems.equipment.presentation.mapper;

import com.ensolution.ems.equipment.application.command.RecordInspectionCommand;
import com.ensolution.ems.equipment.domain.InspectionRecord;
import com.ensolution.ems.equipment.presentation.request.RecordInspectionRequest;
import com.ensolution.ems.equipment.presentation.response.InspectionRecordResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface InspectionRecordMapper {

	@Mapping(target = "equipmentId", source = "equipmentId")
	@Mapping(target = "tenantId", source = "tenantId")
	RecordInspectionCommand toRecordCommand(
		RecordInspectionRequest request, String equipmentId, Long tenantId
	);

	@Mapping(
		target = "typeLabel",
		expression = "java(inspectionRecord.getType() == null ? null : inspectionRecord.getType().getDesc())"
	)
	InspectionRecordResponse toResponse(InspectionRecord inspectionRecord);

	List<InspectionRecordResponse> toResponses(List<InspectionRecord> inspectionRecords);
}
