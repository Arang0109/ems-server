package com.ensolution.ems.tenant.infrastructure.mapper;

import com.ensolution.ems.tenant.domain.Client;
import com.ensolution.ems.tenant.infrastructure.entity.ClientEntity;
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

	@Mapping(target = "clientId", source = "id")
	@Mapping(target = "tenant", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	ClientEntity toEntity(Client client);

	@Mapping(target = "id", source = "clientId")
	@Mapping(target = "tenantId", source = "tenant.tenantId")
	Client toDomain(ClientEntity client);

	List<Client> toDomainList(List<ClientEntity> clients);
}
