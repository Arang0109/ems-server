package com.ensolution.ems.tenant.presentation.stack_pollutant.mapper;

import com.ensolution.ems.tenant.application.command.create.CreateStackPollutantCommand;
import com.ensolution.ems.tenant.application.command.list_item.StackPollutantListItem;
import com.ensolution.ems.tenant.domain.StackPollutant;
import com.ensolution.ems.tenant.presentation.stack_pollutant.request.CreateStackPollutantRequest;
import com.ensolution.ems.tenant.presentation.stack_pollutant.response.StackPollutantResponse;
import com.ensolution.ems.tenant.presentation.stack_pollutant.response.StackPollutantListResponse;
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
	StackPollutantResponse toResponse(StackPollutant stackPollutant);
	
	StackPollutantListResponse toListResponse(StackPollutantListItem item);
	List<StackPollutantListResponse> toListResponses(List<StackPollutantListItem> items);
}
