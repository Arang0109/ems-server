package com.ensolution.ems.tenant.infrastructure.mapper;

import com.ensolution.ems.tenant.application.command.list_item.WorkplaceListItem;
import com.ensolution.ems.tenant.domain.Workplace;
import com.ensolution.ems.tenant.infrastructure.entity.WorkplaceEntity;
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

    @Mapping(target = "workplaceId", source = "id")
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    WorkplaceEntity toEntity(Workplace workplace);

    @Mapping(target = "id", source = "workplaceId")
    @Mapping(target = "tenantId", source = "tenant.tenantId")
    @Mapping(target = "clientId", source = "client.clientId")
    Workplace toDomain(WorkplaceEntity entity);

		@Mapping(target = "id", source = "workplaceId")
		@Mapping(target = "clientId", source = "client.clientId")
		@Mapping(target = "clientName", source = "client.name")
		WorkplaceListItem toWorkplaceListItem(WorkplaceEntity entity);

    List<WorkplaceListItem> toWorkplaceListItems(List<WorkplaceEntity> entities);
}
