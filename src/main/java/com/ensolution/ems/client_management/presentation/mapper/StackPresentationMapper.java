package com.ensolution.ems.client_management.presentation.mapper;

import com.ensolution.ems.client_management.application.command.CreateStackCommand;
import com.ensolution.ems.client_management.application.command.UpdateStackCommand;
import com.ensolution.ems.client_management.application.command.StackListItem;
import com.ensolution.ems.client_management.domain.Stack;
import com.ensolution.ems.client_management.presentation.request.CreateStackRequest;
import com.ensolution.ems.client_management.presentation.request.UpdateStackRequest;
import com.ensolution.ems.client_management.presentation.response.StackListResponse;
import com.ensolution.ems.client_management.presentation.response.StackResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
		componentModel = "spring",
		builder = @Builder
)
public interface StackPresentationMapper {
	CreateStackCommand toCreateCommand(CreateStackRequest request);
	UpdateStackCommand toUpdateCommand(UpdateStackRequest request);
	StackResponse toResponse(Stack stack);
	
	@Mapping(target = "stackName", source = "name")
	StackListResponse toListResponse(StackListItem stackListItem);

	List<StackListResponse> toListResponses(List<StackListItem> items);
}
