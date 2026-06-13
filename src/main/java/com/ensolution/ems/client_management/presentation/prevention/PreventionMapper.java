package com.ensolution.ems.client_management.presentation.prevention;

import com.ensolution.ems.client_management.application.command.create.CreatePreventionCommand;
import com.ensolution.ems.client_management.application.command.update.UpdatePreventionCommand;
import com.ensolution.ems.client_management.domain.Prevention;
import com.ensolution.ems.client_management.presentation.target_substance.TargetSubstanceMapper;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

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
	PreventionDetailResponse toDetailResponse(Prevention prevention);
	List<PreventionResponse> toResponses(List<Prevention> preventions);
}
