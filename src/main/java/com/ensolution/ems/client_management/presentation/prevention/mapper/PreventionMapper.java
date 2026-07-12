package com.ensolution.ems.client_management.presentation.prevention.mapper;

import com.ensolution.ems.client_management.application.command.create.CreatePreventionCommand;
import com.ensolution.ems.client_management.application.command.detail.PreventionDetail;
import com.ensolution.ems.client_management.application.command.update.UpdatePreventionCommand;
import com.ensolution.ems.client_management.domain.Prevention;
import com.ensolution.ems.client_management.presentation.prevention.request.CreatePreventionRequest;
import com.ensolution.ems.client_management.presentation.prevention.request.UpdatePreventionRequest;
import com.ensolution.ems.client_management.presentation.prevention.response.PreventionDetailResponse;
import com.ensolution.ems.client_management.presentation.prevention.response.PreventionResponse;
import com.ensolution.ems.client_management.presentation.target_substance.mapper.TargetSubstanceMapper;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder,
	uses = {
		TargetSubstanceMapper.class
	}
)
public interface PreventionMapper {
	CreatePreventionCommand toCreateCommand(CreatePreventionRequest request);
	UpdatePreventionCommand toUpdateCommand(UpdatePreventionRequest request);
	PreventionResponse toResponse(Prevention prevention);
	List<PreventionResponse> toResponses(List<Prevention> preventions);

	@Mapping(target = "id", source = "prevention.id")
	@Mapping(target = "stackId", source = "prevention.stackId")
	@Mapping(target = "name", source = "prevention.name")
	@Mapping(target = "targets", source = "targets")
	PreventionDetailResponse toDetailResponse(PreventionDetail detail);
}
