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
public interface PreventionEntityMapper {
	@Mapping(target = "stack", ignore = true)
	@Mapping(target = "targets", ignore = true)
	PreventionEntity toEntity(Prevention prevention);

	@Mapping(target = "stackId", source = "stack.id")
	Prevention toDomain(PreventionEntity entity);

	List<Prevention> toDomainList(List<PreventionEntity> entities);
}
