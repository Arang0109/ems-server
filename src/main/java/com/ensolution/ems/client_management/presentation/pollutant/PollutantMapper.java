package com.ensolution.ems.client_management.presentation.pollutant;

import com.ensolution.ems.client_management.application.command.create.CreatePollutantCommand;
import com.ensolution.ems.client_management.application.command.update.UpdatePollutantCommand;
import com.ensolution.ems.client_management.domain.Pollutant;
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
