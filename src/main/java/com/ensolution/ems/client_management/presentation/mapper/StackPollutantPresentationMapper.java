package com.ensolution.ems.client_management.presentation.mapper;

import com.ensolution.ems.client_management.application.command.AssignStackPollutantCommand;
import com.ensolution.ems.client_management.application.command.StackPollutantListItem;
import com.ensolution.ems.client_management.presentation.request.AssignStackPollutantRequest;
import com.ensolution.ems.client_management.presentation.response.StackPollutantResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface StackPollutantPresentationMapper {
	AssignStackPollutantCommand toAssignCommand(AssignStackPollutantRequest request);
	StackPollutantResponse toResponse(StackPollutantListItem item);
	List<StackPollutantResponse> toResponses(List<StackPollutantListItem> items);
}
