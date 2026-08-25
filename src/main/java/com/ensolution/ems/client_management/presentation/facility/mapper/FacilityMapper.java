package com.ensolution.ems.client_management.presentation.facility.mapper;

import com.ensolution.ems.client_management.application.command.create.CreateFacilityCommand;
import com.ensolution.ems.client_management.application.command.update.ReorderFacilitiesCommand;
import com.ensolution.ems.client_management.application.command.update.UpdateFacilityCommand;
import com.ensolution.ems.client_management.domain.Facility;
import com.ensolution.ems.client_management.presentation.facility.request.CreateFacilityRequest;
import com.ensolution.ems.client_management.presentation.facility.request.ReorderFacilitiesRequest;
import com.ensolution.ems.client_management.presentation.facility.response.FacilityResponse;
import com.ensolution.ems.client_management.presentation.facility.request.UpdateFacilityRequest;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder)
public interface FacilityMapper {
	@Mapping(target = "tenantId", source = "tenantId")
	CreateFacilityCommand toCreateCommand(CreateFacilityRequest request, Long tenantId);
	UpdateFacilityCommand toUpdateCommand(UpdateFacilityRequest request);

	@Mapping(target = "tenantId", source = "tenantId")
	ReorderFacilitiesCommand toReorderCommand(ReorderFacilitiesRequest request, Long tenantId);

	FacilityResponse toResponse(Facility facility);
	List<FacilityResponse> toResponses(List<Facility> facilities);
}
