package com.ensolution.ems.client_management.presentation.pollutant_catalog.mapper;

import com.ensolution.ems.client_management.application.command.create.CreatePollutantCatalogCommand;
import com.ensolution.ems.client_management.application.command.update.UpdatePollutantCatalogCommand;
import com.ensolution.ems.client_management.domain.PollutantCatalog;
import com.ensolution.ems.client_management.presentation.pollutant_catalog.request.CreatePollutantCatalogRequest;
import com.ensolution.ems.client_management.presentation.pollutant_catalog.request.UpdatePollutantCatalogRequest;
import com.ensolution.ems.client_management.presentation.pollutant_catalog.response.PollutantCatalogResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface PollutantCatalogMapper {
	CreatePollutantCatalogCommand toCreateCommand(CreatePollutantCatalogRequest request);

	UpdatePollutantCatalogCommand toUpdateCommand(UpdatePollutantCatalogRequest request);

	PollutantCatalogResponse toResponse(PollutantCatalog catalog);

	List<PollutantCatalogResponse> toResponses(List<PollutantCatalog> catalogs);
}
