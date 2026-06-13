package com.ensolution.ems.client_management.infrastructure.mapper;

import com.ensolution.ems.client_management.application.command.list_item.StackListItem;
import com.ensolution.ems.client_management.domain.Stack;
import com.ensolution.ems.client_management.infrastructure.entity.StackEntity;
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
		PreventionEntityMapper.class,
		FacilityEntityMapper.class
	}
)
public interface StackEntityMapper {

	@Mapping(target = "workplace", ignore = true)
	@Mapping(target = "preventions", ignore = true)
	@Mapping(target = "facilities", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	StackEntity toEntity(Stack stack);

	@Mapping(target = "workplaceId", source = "workplace.id")
	Stack toDomain(StackEntity entity);

	@Mapping(target = "companyName", source = "workplace.company.name")
	@Mapping(target = "workplaceName", source = "workplace.name")
	StackListItem toStackListItem(StackEntity entity);
	List<StackListItem> toStackListItems(List<StackEntity> entities);
}
