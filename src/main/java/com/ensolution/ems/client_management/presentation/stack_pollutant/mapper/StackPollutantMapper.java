package com.ensolution.ems.client_management.presentation.stack_pollutant.mapper;

import com.ensolution.ems.client_management.application.command.create.CreateStackPollutantCommand;
import com.ensolution.ems.client_management.application.command.list_item.StackPollutantListItem;
import com.ensolution.ems.client_management.domain.StackPollutant;
import com.ensolution.ems.client_management.presentation.stack_pollutant.request.CreateStackPollutantRequest;
import com.ensolution.ems.client_management.presentation.stack_pollutant.response.StackPollutantResponse;
import com.ensolution.ems.client_management.presentation.stack_pollutant.response.StackPollutantTableListResponse;
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
	
	StackPollutantTableListResponse toListResponse(StackPollutantListItem item);
	List<StackPollutantTableListResponse> toListResponses(List<StackPollutantListItem> items);
}
