package com.ensolution.ems.client_management.presentation.mapper;

import com.ensolution.ems.client_management.application.command.CreatePollutantCommand;
import com.ensolution.ems.client_management.application.command.UpdatePollutantCommand;
import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.client_management.presentation.request.CreatePollutantRequest;
import com.ensolution.ems.client_management.presentation.request.UpdatePollutantRequest;
import com.ensolution.ems.client_management.presentation.response.PollutantResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface PollutantPresentationMapper {
	CreatePollutantCommand toCreateCommand(CreatePollutantRequest request);
	UpdatePollutantCommand toUpdateCommand(UpdatePollutantRequest request);
	PollutantResponse toResponse(Pollutant pollutant);
	List<PollutantResponse> toResponses(List<Pollutant> pollutants);
}
