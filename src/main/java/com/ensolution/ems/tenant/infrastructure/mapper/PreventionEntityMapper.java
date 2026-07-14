package com.ensolution.ems.tenant.infrastructure.mapper;

import com.ensolution.ems.tenant.domain.Prevention;
import com.ensolution.ems.tenant.infrastructure.entity.PreventionEntity;
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
public interface PreventionEntityMapper {
	@Mapping(target = "preventionId", source = "id")
	@Mapping(target = "tenant", ignore = true)
	@Mapping(target = "stack", ignore = true)
	@Mapping(target = "targetSubstances", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	PreventionEntity toEntity(Prevention prevention);

	@Mapping(target = "id", source = "preventionId")
	@Mapping(target = "tenantId", source = "tenant.tenantId")
	@Mapping(target = "stackId", source = "stack.stackId")
	Prevention toDomain(PreventionEntity entity);

	List<Prevention> toDomainList(List<PreventionEntity> entities);
}
