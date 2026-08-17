package com.ensolution.ems.client_management.infrastructure.mapper;

import com.ensolution.ems.client_management.domain.PollutantCatalog;
import com.ensolution.ems.client_management.infrastructure.entity.PollutantCatalogEntity;
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
public interface PollutantCatalogEntityMapper {

	@Mapping(target = "catalogId", source = "id")
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	PollutantCatalogEntity toEntity(PollutantCatalog catalog);

	@Mapping(target = "id", source = "catalogId")
	PollutantCatalog toDomain(PollutantCatalogEntity entity);

	List<PollutantCatalog> toDomainList(List<PollutantCatalogEntity> entities);
}
