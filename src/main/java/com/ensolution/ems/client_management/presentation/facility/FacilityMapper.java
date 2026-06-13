package com.ensolution.ems.client_management.presentation.facility;

import com.ensolution.ems.client_management.application.command.create.CreateFacilityCommand;
import com.ensolution.ems.client_management.application.command.update.UpdateFacilityCommand;
import com.ensolution.ems.client_management.domain.Facility;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder)
public interface FacilityMapper {
	CreateFacilityCommand toCreateCommand(CreateFacilityRequest request);
	UpdateFacilityCommand toUpdateCommand(UpdateFacilityRequest request);
	FacilityResponse toResponse(Facility facility);
	List<FacilityResponse> toResponses(List<Facility> facilities);
}
