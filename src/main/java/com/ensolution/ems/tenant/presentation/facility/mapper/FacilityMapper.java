package com.ensolution.ems.tenant.presentation.facility.mapper;

import com.ensolution.ems.tenant.application.command.create.CreateFacilityCommand;
import com.ensolution.ems.tenant.application.command.update.UpdateFacilityCommand;
import com.ensolution.ems.tenant.domain.Facility;
import com.ensolution.ems.tenant.presentation.facility.request.CreateFacilityRequest;
import com.ensolution.ems.tenant.presentation.facility.response.FacilityResponse;
import com.ensolution.ems.tenant.presentation.facility.request.UpdateFacilityRequest;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder)
public interface FacilityMapper {
	@Mapping(target = "tenantId", source = "tenantId")
	CreateFacilityCommand toCreateCommand(CreateFacilityRequest request, Long tenantId);
	UpdateFacilityCommand toUpdateCommand(UpdateFacilityRequest request);
	FacilityResponse toResponse(Facility facility);
	List<FacilityResponse> toResponses(List<Facility> facilities);
}
