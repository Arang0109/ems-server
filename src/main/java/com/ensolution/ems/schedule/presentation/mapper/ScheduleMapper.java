package com.ensolution.ems.schedule.presentation.mapper;

import com.ensolution.ems.schedule.application.command.create.CreateScheduleCommand;
import com.ensolution.ems.schedule.application.command.detail.ScheduleDetail;
import com.ensolution.ems.schedule.application.command.list_item.ScheduleListItem;
import com.ensolution.ems.schedule.application.command.update.ChangeScheduleEquipmentsCommand;
import com.ensolution.ems.schedule.application.command.update.ChangeClientSnapshotCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateBasicInfoCommand;
import com.ensolution.ems.schedule.application.command.update.UpdateScheduleCommand;
import com.ensolution.ems.schedule.domain.ScheduleStatusLog;
import com.ensolution.ems.schedule.presentation.request.ChangeScheduleEquipmentsRequest;
import com.ensolution.ems.schedule.presentation.request.ChangeClientSnapshotRequest;
import com.ensolution.ems.schedule.presentation.request.CreateScheduleRequest;
import com.ensolution.ems.schedule.presentation.request.UpdateBasicInfoRequest;
import com.ensolution.ems.schedule.presentation.request.UpdateScheduleRequest;
import com.ensolution.ems.schedule.presentation.response.ScheduleListResponse;
import com.ensolution.ems.schedule.presentation.response.ScheduleResponse;
import com.ensolution.ems.schedule.presentation.response.ScheduleStatusLogResponse;
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
	@Mapping(target = "registeredBy", source = "registeredBy")
	CreateScheduleCommand toCreateCommand(CreateScheduleRequest request, Long tenantId, Long registeredBy);

	UpdateScheduleCommand toUpdateCommand(UpdateScheduleRequest request);

	ChangeScheduleEquipmentsCommand toChangeEquipmentsCommand(ChangeScheduleEquipmentsRequest request);

	ChangeClientSnapshotCommand toChangeClientCommand(ChangeClientSnapshotRequest request);

	UpdateBasicInfoCommand toUpdateBasicInfoCommand(UpdateBasicInfoRequest request);

	@Mapping(target = "id", source = "meta.id")
	@Mapping(target = "tenantId", source = "meta.tenantId")
	@Mapping(target = "stackId", source = "meta.stackId")
	@Mapping(target = "teamId", source = "meta.teamId")
	@Mapping(target = "measurementField", source = "meta.measurementField")
	@Mapping(target = "sampledAt", source = "meta.sampledAt")
	@Mapping(target = "schedulePurpose", source = "meta.schedulePurpose")
	@Mapping(target = "status", source = "meta.status")
	@Mapping(target = "referenceNumber", source = "meta.referenceNumber")
	@Mapping(target = "createdAt", source = "meta.createdAt")
	@Mapping(target = "modifiedAt", source = "meta.modifiedAt")
	@Mapping(target = "snapshot", source = "snapshot")
	ScheduleResponse toResponse(ScheduleDetail detail);

	List<ScheduleListResponse> toListResponses(List<ScheduleListItem> items);

	ScheduleStatusLogResponse toStatusLogResponse(ScheduleStatusLog log);

	List<ScheduleStatusLogResponse> toStatusLogResponses(List<ScheduleStatusLog> logs);
}
