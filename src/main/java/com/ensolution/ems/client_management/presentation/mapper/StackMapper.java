package com.ensolution.ems.client_management.presentation.mapper;

import com.ensolution.ems.client_management.application.command.create.CreateStackCommand;
import com.ensolution.ems.client_management.application.command.update.UpdateStackCommand;
import com.ensolution.ems.client_management.application.command.list_item.StackListItem;
import com.ensolution.ems.client_management.domain.Stack;
import com.ensolution.ems.client_management.presentation.request.create.CreateStackRequest;
import com.ensolution.ems.client_management.presentation.request.update.UpdateStackRequest;
import com.ensolution.ems.client_management.presentation.response.table.StackTableResponse;
import com.ensolution.ems.client_management.presentation.response.StackResponse;
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
	
	@Mapping(target = "stackName", source = "name")
	StackTableResponse toListResponse(StackListItem stackListItem);

	List<StackTableResponse> toListResponses(List<StackListItem> items);
}
