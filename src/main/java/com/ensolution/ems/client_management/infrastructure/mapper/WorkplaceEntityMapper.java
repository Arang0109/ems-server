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

    @Mapping(target = "client", ignore = true)
    @Mapping(target = "stacks", ignore = true)
    WorkplaceEntity toEntity(Workplace workplace);

    @Mapping(target = "clientId", source = "client.id")
    Workplace toDomain(WorkplaceEntity entity);

		@Mapping(target = "clientId", source = "client.id")
		@Mapping(target = "clientName", source = "client.name")
		WorkplaceListItem toWorkplaceListItem(WorkplaceEntity entity);

    List<WorkplaceListItem> toWorkplaceListItems(List<WorkplaceEntity> entities);
}
