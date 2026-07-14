package com.ensolution.ems.schedule.presentation.mapper;

import com.ensolution.ems.schedule.application.command.create.CreateScheduleCommand;
import com.ensolution.ems.schedule.application.command.detail.ScheduleDetail;
import com.ensolution.ems.schedule.application.command.list_item.ScheduleListItem;
import com.ensolution.ems.schedule.application.command.update.UpdateScheduleCommand;
import com.ensolution.ems.schedule.presentation.request.CreateScheduleRequest;
import com.ensolution.ems.schedule.presentation.request.UpdateScheduleRequest;
import com.ensolution.ems.schedule.presentation.response.ScheduleListResponse;
import com.ensolution.ems.schedule.presentation.response.ScheduleResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface ScheduleMapper {

	@Mapping(target = "tenantId", source = "tenantId")
	CreateScheduleCommand toCreateCommand(CreateScheduleRequest request, Long tenantId);

	UpdateScheduleCommand toUpdateCommand(UpdateScheduleRequest request);

	@Mapping(target = "id", source = "meta.id")
	@Mapping(target = "tenantId", source = "meta.tenantId")
	@Mapping(target = "stackId", source = "meta.stackId")
	@Mapping(target = "teamId", source = "meta.teamId")
	@Mapping(target = "measurementField", source = "meta.measurementField")
	@Mapping(target = "measureDate", source = "meta.measureDate")
	@Mapping(target = "measurementType", source = "meta.measurementType")
	@Mapping(target = "status", source = "meta.status")
	@Mapping(target = "referenceNumber", source = "meta.referenceNumber")
	@Mapping(target = "createdAt", source = "meta.createdAt")
	@Mapping(target = "modifiedAt", source = "meta.modifiedAt")
	@Mapping(target = "snapshot", source = "snapshot")
	ScheduleResponse toResponse(ScheduleDetail detail);

	List<ScheduleListResponse> toListResponses(List<ScheduleListItem> items);
}
