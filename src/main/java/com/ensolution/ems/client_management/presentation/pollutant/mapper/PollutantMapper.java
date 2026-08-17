package com.ensolution.ems.client_management.presentation.pollutant.mapper;

import com.ensolution.ems.client_management.application.command.create.CreatePollutantCommand;
import com.ensolution.ems.client_management.application.command.list_item.PollutantListItem;
import com.ensolution.ems.client_management.application.command.update.UpdatePollutantCommand;
import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.client_management.presentation.pollutant.response.PollutantListResponse;
import com.ensolution.ems.client_management.presentation.pollutant.response.PollutantResponse;
import com.ensolution.ems.client_management.presentation.pollutant.request.CreatePollutantRequest;
import com.ensolution.ems.client_management.presentation.pollutant.request.UpdatePollutantRequest;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface PollutantMapper {
	@Mapping(target = "tenantId", source = "tenantId")
	CreatePollutantCommand toCreateCommand(CreatePollutantRequest request, Long tenantId);

	UpdatePollutantCommand toUpdateCommand(UpdatePollutantRequest request);

	PollutantResponse toResponse(Pollutant pollutant);

	PollutantListResponse toListResponse(PollutantListItem item);

	List<PollutantListResponse> toListResponses(List<PollutantListItem> items);
}
