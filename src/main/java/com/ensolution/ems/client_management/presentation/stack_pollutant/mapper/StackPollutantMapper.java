package com.ensolution.ems.client_management.presentation.stack_pollutant.mapper;

import com.ensolution.ems.client_management.application.command.create.CreateStackPollutantCommand;
import com.ensolution.ems.client_management.application.command.list_item.StackPollutantListItem;
import com.ensolution.ems.client_management.application.command.update.UpdateStackPollutantCommand;
import com.ensolution.ems.client_management.domain.StackPollutant;
import com.ensolution.ems.client_management.presentation.stack_pollutant.request.CreateStackPollutantRequest;
import com.ensolution.ems.client_management.presentation.stack_pollutant.request.UpdateStackPollutantRequest;
import com.ensolution.ems.client_management.presentation.stack_pollutant.response.StackPollutantResponse;
import com.ensolution.ems.client_management.presentation.stack_pollutant.response.StackPollutantListResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface StackPollutantMapper {
	@Mapping(target = "tenantId", source = "tenantId")
	CreateStackPollutantCommand toCreateCommand(CreateStackPollutantRequest request, Long tenantId);

	default List<CreateStackPollutantCommand> toCreateCommands(List<CreateStackPollutantRequest> requests, Long tenantId) {
		return requests.stream().map(request -> toCreateCommand(request, tenantId)).toList();
	}

	UpdateStackPollutantCommand toUpdateCommand(UpdateStackPollutantRequest request);

	StackPollutantResponse toResponse(StackPollutant stackPollutant);
	List<StackPollutantResponse> toResponses(List<StackPollutant> stackPollutants);

	StackPollutantListResponse toListResponse(StackPollutantListItem item);
	List<StackPollutantListResponse> toListResponses(List<StackPollutantListItem> items);
}
