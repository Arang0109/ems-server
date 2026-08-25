package com.ensolution.ems.client_management.presentation.prevention.mapper;

import com.ensolution.ems.client_management.application.command.create.CreatePreventionCommand;
import com.ensolution.ems.client_management.application.command.update.ReorderPreventionsCommand;
import com.ensolution.ems.client_management.application.command.update.UpdatePreventionCommand;
import com.ensolution.ems.client_management.domain.Prevention;
import com.ensolution.ems.client_management.presentation.prevention.request.CreatePreventionRequest;
import com.ensolution.ems.client_management.presentation.prevention.request.ReorderPreventionsRequest;
import com.ensolution.ems.client_management.presentation.prevention.request.UpdatePreventionRequest;
import com.ensolution.ems.client_management.presentation.prevention.response.PreventionResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface PreventionMapper {
	@Mapping(target = "tenantId", source = "tenantId")
	CreatePreventionCommand toCreateCommand(CreatePreventionRequest request, Long tenantId);
	UpdatePreventionCommand toUpdateCommand(UpdatePreventionRequest request);

	@Mapping(target = "tenantId", source = "tenantId")
	ReorderPreventionsCommand toReorderCommand(ReorderPreventionsRequest request, Long tenantId);

	PreventionResponse toResponse(Prevention prevention);
	List<PreventionResponse> toResponses(List<Prevention> preventions);
}
