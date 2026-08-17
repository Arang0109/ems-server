package com.ensolution.ems.client_management.infrastructure.mapper;

import com.ensolution.ems.client_management.application.command.list_item.StackPollutantListItem;
import com.ensolution.ems.client_management.domain.StackPollutant;
import com.ensolution.ems.client_management.infrastructure.entity.StackPollutantEntity;
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
public interface StackPollutantEntityMapper {

	@Mapping(target = "stackPollutantId", source = "id")
	@Mapping(target = "tenant", ignore = true)
	@Mapping(target = "stack", ignore = true)
	@Mapping(target = "pollutant", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	StackPollutantEntity toEntity(StackPollutant stackPollutant);

	@Mapping(target = "id", source = "stackPollutantId")
	@Mapping(target = "tenantId", source = "tenant.tenantId")
	@Mapping(target = "stackId", source = "stack.stackId")
	@Mapping(target = "pollutantId", source = "pollutant.pollutantId")
	StackPollutant toDomain(StackPollutantEntity entity);

	/**
	 * 이름은 원장 값 그대로 담는다. 카탈로그를 상속한 실효값 치환은
	 * {@code PollutantCatalogAssembler}를 거치는 서비스가 담당한다.
	 */
	@Mapping(target = "id", source = "stackPollutantId")
	@Mapping(target = "stackId", source = "stack.stackId")
	@Mapping(target = "pollutantId", source = "pollutant.pollutantId")
	@Mapping(target = "code", source = "pollutant.catalog.code")
	@Mapping(target = "nameKr", source = "pollutant.nameKr")
	@Mapping(target = "nameEn", source = "pollutant.nameEn")
	StackPollutantListItem toListItem(StackPollutantEntity entity);

	List<StackPollutantListItem> toListItems(List<StackPollutantEntity> entities);
}
