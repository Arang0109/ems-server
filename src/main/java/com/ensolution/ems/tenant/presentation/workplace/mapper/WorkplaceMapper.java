package com.ensolution.ems.tenant.presentation.workplace.mapper;

import com.ensolution.ems.tenant.application.command.create.CreateWorkplaceCommand;
import com.ensolution.ems.tenant.application.command.update.UpdateWorkplaceCommand;
import com.ensolution.ems.tenant.application.command.list_item.WorkplaceListItem;
import com.ensolution.ems.tenant.domain.Workplace;
import com.ensolution.ems.tenant.presentation.workplace.request.CreateWorkplaceRequest;
import com.ensolution.ems.tenant.presentation.workplace.request.UpdateWorkplaceRequest;
import com.ensolution.ems.tenant.presentation.workplace.response.WorkplaceResponse;
import com.ensolution.ems.tenant.presentation.workplace.response.WorkplaceListResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
		componentModel = "spring",
		builder = @Builder
)
public interface WorkplaceMapper {
	@Mapping(target = "tenantId", source = "tenantId")
	CreateWorkplaceCommand toCreateCommand(CreateWorkplaceRequest request, Long tenantId);
	UpdateWorkplaceCommand toUpdateCommand(UpdateWorkplaceRequest request);

	WorkplaceResponse toResponse(Workplace workplace);
	
	@Mapping(target = "workplaceName", source = "name")
	WorkplaceListResponse toListResponse(WorkplaceListItem workplaceListItem);

	List<WorkplaceListResponse> toListResponses(List<WorkplaceListItem> items);
}
