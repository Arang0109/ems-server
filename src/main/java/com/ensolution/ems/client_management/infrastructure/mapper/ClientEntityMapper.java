package com.ensolution.ems.client_management.infrastructure.mapper;

import com.ensolution.ems.client_management.domain.Client;
import com.ensolution.ems.client_management.infrastructure.entity.ClientEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
		componentModel = "spring",
		builder = @Builder(),
		unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ClientEntityMapper {
	@Mapping(target = "workplaces", ignore = true)
	ClientEntity toEntity(Client client);

	Client toDomain(ClientEntity client);

	List<Client> toDomainList(List<ClientEntity> clients);
}
