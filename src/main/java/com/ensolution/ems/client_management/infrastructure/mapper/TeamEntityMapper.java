package com.ensolution.ems.client_management.infrastructure.mapper;

import com.ensolution.ems.client_management.domain.Team;
import com.ensolution.ems.client_management.infrastructure.entity.TeamEntity;
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
public interface TeamEntityMapper {

	@Mapping(target = "teamId", source = "id")
	@Mapping(target = "tenant", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	TeamEntity toEntity(Team team);

	@Mapping(target = "id", source = "teamId")
	@Mapping(target = "tenantId", source = "tenant.tenantId")
	Team toDomain(TeamEntity entity);

	List<Team> toDomainList(List<TeamEntity> entities);
}
