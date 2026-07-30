package com.ensolution.ems.client_management.presentation.client.mapper;

import com.ensolution.ems.client_management.application.command.create.CreateClientCommand;
import com.ensolution.ems.client_management.application.command.update.UpdateClientCommand;
import com.ensolution.ems.client_management.domain.Client;
import com.ensolution.ems.client_management.presentation.client.response.ClientResponse;
import com.ensolution.ems.client_management.presentation.client.request.CreateClientRequest;
import com.ensolution.ems.client_management.presentation.client.request.UpdateClientRequest;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
		componentModel = "spring",
		builder = @Builder
)
public interface ClientMapper {
	@Mapping(target = "tenantId", source = "tenantId")
	CreateClientCommand toCreateCommand(CreateClientRequest request, Long tenantId);
	UpdateClientCommand toUpdateCommand(UpdateClientRequest request);
	ClientResponse toResponse(Client client);
	List<ClientResponse> toResponses(List<Client> clients);
}
