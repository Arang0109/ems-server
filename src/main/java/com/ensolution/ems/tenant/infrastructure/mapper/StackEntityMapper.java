package com.ensolution.ems.tenant.infrastructure.mapper;

import com.ensolution.ems.tenant.application.command.list_item.StackListItem;
import com.ensolution.ems.tenant.domain.Stack;
import com.ensolution.ems.tenant.infrastructure.entity.StackEntity;
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
public interface StackEntityMapper {

	@Mapping(target = "stackId", source = "id")
	@Mapping(target = "tenant", ignore = true)
	@Mapping(target = "workplace", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	StackEntity toEntity(Stack stack);

	@Mapping(target = "id", source = "stackId")
	@Mapping(target = "tenantId", source = "tenant.tenantId")
	@Mapping(target = "workplaceId", source = "workplace.workplaceId")
	Stack toDomain(StackEntity entity);

	@Mapping(target = "id", source = "stackId")
	@Mapping(target = "clientName", source = "workplace.client.name")
	@Mapping(target = "workplaceName", source = "workplace.name")
	StackListItem toStackListItem(StackEntity entity);
	List<StackListItem> toStackListItems(List<StackEntity> entities);
}
