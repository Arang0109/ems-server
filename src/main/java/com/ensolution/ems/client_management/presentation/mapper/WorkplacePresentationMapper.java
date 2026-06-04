package com.ensolution.ems.client_management.presentation.mapper;

import com.ensolution.ems.client_management.application.command.CreateWorkplaceCommand;
import com.ensolution.ems.client_management.application.command.WorkplaceListItem;
import com.ensolution.ems.client_management.domain.Workplace;
import com.ensolution.ems.client_management.presentation.request.CreateWorkplaceRequest;
import com.ensolution.ems.client_management.presentation.response.WorkplaceResponse;
import com.ensolution.ems.client_management.presentation.response.WorkplaceListResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
		componentModel = "spring",
		builder = @Builder
)
public interface WorkplacePresentationMapper {
	CreateWorkplaceCommand toCommand(CreateWorkplaceRequest request);
	WorkplaceResponse toResponse(Workplace workplace);
	
	@Mapping(target = "workplaceName", source = "name")
	WorkplaceListResponse toListResponse(WorkplaceListItem workplaceListItem);

	List<WorkplaceListResponse> toListResponses(List<WorkplaceListItem> items);
}
