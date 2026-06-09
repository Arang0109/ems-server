package com.ensolution.ems.client_management.presentation.mapper;

import com.ensolution.ems.client_management.application.command.create.CreatePollutantCommand;
import com.ensolution.ems.client_management.application.command.update.UpdatePollutantCommand;
import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.client_management.presentation.request.create.CreatePollutantRequest;
import com.ensolution.ems.client_management.presentation.request.update.UpdatePollutantRequest;
import com.ensolution.ems.client_management.presentation.response.PollutantResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface PollutantMapper {
	CreatePollutantCommand toCreateCommand(CreatePollutantRequest request);
	UpdatePollutantCommand toUpdateCommand(UpdatePollutantRequest request);
	PollutantResponse toResponse(Pollutant pollutant);
	List<PollutantResponse> toResponses(List<Pollutant> pollutants);
}
