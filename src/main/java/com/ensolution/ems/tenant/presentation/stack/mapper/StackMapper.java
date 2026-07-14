package com.ensolution.ems.tenant.presentation.stack.mapper;

import com.ensolution.ems.tenant.application.command.create.CreateStackCommand;
import com.ensolution.ems.tenant.application.command.detail.StackDetail;
import com.ensolution.ems.tenant.application.command.update.UpdateStackCommand;
import com.ensolution.ems.tenant.application.command.list_item.StackListItem;
import com.ensolution.ems.tenant.domain.Stack;
import com.ensolution.ems.tenant.presentation.facility.mapper.FacilityMapper;
import com.ensolution.ems.tenant.presentation.prevention.mapper.PreventionMapper;
import com.ensolution.ems.tenant.presentation.stack.request.CreateStackRequest;
import com.ensolution.ems.tenant.presentation.stack.response.StackDetailResponse;
import com.ensolution.ems.tenant.presentation.stack.response.StackResponse;
import com.ensolution.ems.tenant.presentation.stack.response.StackListResponse;
import com.ensolution.ems.tenant.presentation.stack.request.UpdateStackRequest;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
		componentModel = "spring",
		builder = @Builder,
		uses = {
			PreventionMapper.class,
			FacilityMapper.class
		}
)
public interface StackMapper {
	@Mapping(target = "tenantId", source = "tenantId")
	CreateStackCommand toCreateCommand(CreateStackRequest request, Long tenantId);
	UpdateStackCommand toUpdateCommand(UpdateStackRequest request);
	StackResponse toResponse(Stack stack);

	@Mapping(target = "id", source = "stack.id")
	@Mapping(target = "workplaceId", source = "stack.workplaceId")
	@Mapping(target = "field", source = "stack.field")
	@Mapping(target = "name", source = "stack.name")
	@Mapping(target = "semsNumber", source = "stack.semsNumber")
	@Mapping(target = "grade", source = "stack.grade")
	@Mapping(target = "businessCategory", source = "stack.businessCategory")
	@Mapping(target = "mainProduct", source = "stack.mainProduct")
	@Mapping(target = "height", source = "stack.height")
	@Mapping(target = "horizontalLength", source = "stack.horizontalLength")
	@Mapping(target = "verticalLength", source = "stack.verticalLength")
	@Mapping(target = "shape", source = "stack.shape")
	@Mapping(target = "orientation", source = "stack.orientation")
	@Mapping(target = "createdAt", source = "stack.createdAt")
	@Mapping(target = "modifiedAt", source = "stack.modifiedAt")
	@Mapping(target = "preventions", source = "preventions")
	@Mapping(target = "facilities", source = "facilities")
	StackDetailResponse toDetailResponse(StackDetail detail);

	@Mapping(target = "stackName", source = "name")
	StackListResponse toListResponse(StackListItem stackListItem);

	List<StackListResponse> toListResponses(List<StackListItem> items);
}
