package com.ensolution.ems.client_management.infrastructure.mapper;

import com.ensolution.ems.client_management.application.command.StackListItem;
import com.ensolution.ems.client_management.domain.Stack;
import com.ensolution.ems.client_management.infrastructure.entity.JpaStackEntity;
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
public interface StackDomainEntityMapper {

    @Mapping(target = "workplace", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    JpaStackEntity toEntity(Stack stack);

    @Mapping(target = "workplaceId", source = "workplace.id")
    Stack toDomain(JpaStackEntity entity);
	
		@Mapping(target = "companyName", source = "workplace.company.name")
		@Mapping(target = "workplaceName", source = "workplace.name")
		StackListItem toStackListItem(JpaStackEntity entity);
    List<StackListItem> toStackListItems(List<JpaStackEntity> entities);
}
