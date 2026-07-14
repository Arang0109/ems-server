package com.ensolution.ems.tenant.presentation.target_substance.mapper;

import com.ensolution.ems.tenant.application.command.create.CreateTargetSubstanceCommand;
import com.ensolution.ems.tenant.application.command.update.UpdateTargetSubstanceCommand;
import com.ensolution.ems.tenant.domain.TargetSubstance;
import com.ensolution.ems.tenant.presentation.target_substance.response.TargetSubstanceResponse;
import com.ensolution.ems.tenant.presentation.target_substance.request.CreateTargetSubstanceRequest;
import com.ensolution.ems.tenant.presentation.target_substance.request.UpdateTargetSubstanceRequest;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder)
public interface TargetSubstanceMapper {
	@Mapping(target = "tenantId", source = "tenantId")
	CreateTargetSubstanceCommand toCreateCommand(CreateTargetSubstanceRequest request, Long tenantId);
	UpdateTargetSubstanceCommand toUpdateCommand(UpdateTargetSubstanceRequest request);
	TargetSubstanceResponse toResponse(TargetSubstance targetSubstance);
	List<TargetSubstanceResponse> toResponses(List<TargetSubstance> targetSubstances);
}
