package com.ensolution.ems.client_management.presentation.workplace.mapper;

import com.ensolution.ems.client_management.application.command.create.CreateWorkplaceCommand;
import com.ensolution.ems.client_management.application.command.update.UpdateWorkplaceCommand;
import com.ensolution.ems.client_management.application.command.list_item.WorkplaceListItem;
import com.ensolution.ems.client_management.domain.Workplace;
import com.ensolution.ems.client_management.presentation.workplace.request.CreateWorkplaceRequest;
import com.ensolution.ems.client_management.presentation.workplace.request.UpdateWorkplaceRequest;
import com.ensolution.ems.client_management.presentation.workplace.response.WorkplaceResponse;
import com.ensolution.ems.client_management.presentation.workplace.response.WorkplaceTableListResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
		componentModel = "spring",
		builder = @Builder
)
public interface WorkplaceMapper {
	CreateWorkplaceCommand toCreateCommand(CreateWorkplaceRequest request);
	UpdateWorkplaceCommand toUpdateCommand(UpdateWorkplaceRequest request);
	WorkplaceResponse toResponse(Workplace workplace);
	
	@Mapping(target = "workplaceName", source = "name")
	WorkplaceTableListResponse toTableListResponse(WorkplaceListItem workplaceListItem);

	List<WorkplaceTableListResponse> toTableListResponses(List<WorkplaceListItem> items);
}
