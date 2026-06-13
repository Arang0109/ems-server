package com.ensolution.ems.client_management.presentation.target_substance;

import com.ensolution.ems.client_management.application.command.create.CreateTargetSubstanceCommand;
import com.ensolution.ems.client_management.application.command.update.UpdateTargetSubstanceCommand;
import com.ensolution.ems.client_management.domain.TargetSubstance;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder)
public interface TargetSubstanceMapper {
	CreateTargetSubstanceCommand toCreateCommand(CreateTargetSubstanceRequest request);
	UpdateTargetSubstanceCommand toUpdateCommand(UpdateTargetSubstanceRequest request);
	TargetSubstanceResponse toResponse(TargetSubstance targetSubstance);
	List<TargetSubstanceResponse> toResponses(List<TargetSubstance> targetSubstances);
}
