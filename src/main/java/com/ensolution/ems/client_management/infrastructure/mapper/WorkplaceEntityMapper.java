package com.ensolution.ems.client_management.infrastructure.mapper;

import com.ensolution.ems.client_management.application.command.list_item.WorkplaceListItem;
import com.ensolution.ems.client_management.domain.Workplace;
import com.ensolution.ems.client_management.infrastructure.entity.WorkplaceEntity;
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
public interface WorkplaceEntityMapper {

    @Mapping(target = "company", ignore = true)
    @Mapping(target = "stacks", ignore = true)
    WorkplaceEntity toEntity(Workplace workplace);

    @Mapping(target = "companyId", source = "company.id")
    Workplace toDomain(WorkplaceEntity entity);

		@Mapping(target = "companyId", source = "company.id")
		@Mapping(target = "companyName", source = "company.name")
		WorkplaceListItem toWorkplaceListItem(WorkplaceEntity entity);

    List<WorkplaceListItem> toWorkplaceListItems(List<WorkplaceEntity> entities);
}
