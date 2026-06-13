package com.ensolution.ems.client_management.presentation.stack_pollutant;

import com.ensolution.ems.client_management.application.command.create.CreateStackPollutantCommand;
import com.ensolution.ems.client_management.application.command.list_item.StackPollutantListItem;
import com.ensolution.ems.client_management.domain.StackPollutant;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface StackPollutantMapper {
	CreateStackPollutantCommand toCreateCommand(CreateStackPollutantRequest request);
	StackPollutantResponse toResponse(StackPollutant stackPollutant);
	
	StackPollutantTableResponse toListResponse(StackPollutantListItem item);
	List<StackPollutantTableResponse> toListResponses(List<StackPollutantListItem> items);
}
