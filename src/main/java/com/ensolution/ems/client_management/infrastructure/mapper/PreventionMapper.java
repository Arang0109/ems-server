package com.ensolution.ems.client_management.infrastructure.mapper;

import com.ensolution.ems.client_management.domain.Prevention;
import com.ensolution.ems.client_management.infrastructure.entity.PreventionEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder(),
	unmappedTargetPolicy = ReportingPolicy.ERROR,
	uses = {
		TargetEntityMapper.class
	}
)
public interface PreventionMapper {
	@Mapping(target = "stack", ignore = true)
	@Mapping(target = "targetSubstances", source = "targets")
	PreventionEntity toEntity(Prevention prevention);
	
	@Mapping(target = "stackId", source = "stack.id")
	@Mapping(target = "targets", ignore = true)
	Prevention toDomain(PreventionEntity entity);
	
	List<Prevention> toDomainList(List<PreventionEntity> entities);
}
