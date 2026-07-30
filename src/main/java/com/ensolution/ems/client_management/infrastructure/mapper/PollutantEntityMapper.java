package com.ensolution.ems.client_management.infrastructure.mapper;

import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.client_management.infrastructure.entity.PollutantEntity;
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
public interface PollutantEntityMapper {

	@Mapping(target = "pollutantId", source = "id")
	@Mapping(target = "tenant", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	PollutantEntity toEntity(Pollutant pollutant);

	@Mapping(target = "id", source = "pollutantId")
	@Mapping(target = "tenantId", source = "tenant.tenantId")
	Pollutant toDomain(PollutantEntity entity);

	List<Pollutant> toDomainList(List<PollutantEntity> entities);
}
