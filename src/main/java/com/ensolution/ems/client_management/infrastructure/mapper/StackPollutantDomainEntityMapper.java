package com.ensolution.ems.client_management.infrastructure.mapper;

import com.ensolution.ems.client_management.application.command.StackPollutantListItem;
import com.ensolution.ems.client_management.domain.StackPollutant;
import com.ensolution.ems.client_management.infrastructure.entity.JpaStackPollutantEntity;
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
public interface StackPollutantDomainEntityMapper {

	@Mapping(target = "stack", ignore = true)
	@Mapping(target = "pollutant", ignore = true)
	JpaStackPollutantEntity toEntity(StackPollutant stackPollutant);

	@Mapping(target = "stackId", source = "stack.id")
	@Mapping(target = "pollutantId", source = "pollutant.id")
	StackPollutant toDomain(JpaStackPollutantEntity entity);

	@Mapping(target = "stackId", source = "stack.id")
	@Mapping(target = "pollutantId", source = "pollutant.id")
	@Mapping(target = "nameKr", source = "pollutant.nameKr")
	@Mapping(target = "nameEn", source = "pollutant.nameEn")
	StackPollutantListItem toListItem(JpaStackPollutantEntity entity);

	List<StackPollutantListItem> toListItems(List<JpaStackPollutantEntity> entities);
}
