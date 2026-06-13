package com.ensolution.ems.client_management.presentation.stack.mapper;

import com.ensolution.ems.client_management.application.command.create.CreateStackCommand;
import com.ensolution.ems.client_management.application.command.update.UpdateStackCommand;
import com.ensolution.ems.client_management.application.command.list_item.StackListItem;
import com.ensolution.ems.client_management.domain.Stack;
import com.ensolution.ems.client_management.presentation.stack.request.CreateStackRequest;
import com.ensolution.ems.client_management.presentation.stack.response.StackDetailResponse;
import com.ensolution.ems.client_management.presentation.stack.response.StackResponse;
import com.ensolution.ems.client_management.presentation.stack.response.StackTableResponse;
import com.ensolution.ems.client_management.presentation.stack.request.UpdateStackRequest;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
		componentModel = "spring",
		builder = @Builder
)
public interface StackMapper {
	CreateStackCommand toCreateCommand(CreateStackRequest request);
	UpdateStackCommand toUpdateCommand(UpdateStackRequest request);
	StackResponse toResponse(Stack stack);
	StackDetailResponse toDetailResponse(Stack stack);

	@Mapping(target = "stackName", source = "name")
	StackTableResponse toListResponse(StackListItem stackListItem);

	List<StackTableResponse> toListResponses(List<StackListItem> items);
}
