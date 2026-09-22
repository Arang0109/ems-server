package com.ensolution.ems.schedule.infrastructure.mapper;

import com.ensolution.ems.schedule.domain.custom_field.CustomFieldDefinition;
import com.ensolution.ems.schedule.infrastructure.entity.CustomFieldDefinitionEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder,
	unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface CustomFieldDefinitionEntityMapper {

	@Mapping(target = "fieldId", source = "id")
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	CustomFieldDefinitionEntity toEntity(CustomFieldDefinition definition);

	@Mapping(target = "id", source = "fieldId")
	CustomFieldDefinition toDomain(CustomFieldDefinitionEntity entity);

	List<CustomFieldDefinition> toDomains(List<CustomFieldDefinitionEntity> entities);
}
