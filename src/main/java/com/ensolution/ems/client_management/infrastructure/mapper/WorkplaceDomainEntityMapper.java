package com.ensolution.ems.client_management.infrastructure.mapper;

import com.ensolution.ems.client_management.application.command.WorkplaceListItem;
import com.ensolution.ems.client_management.domain.Workplace;
import com.ensolution.ems.client_management.infrastructure.entity.JpaWorkplaceEntity;
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
public interface WorkplaceDomainEntityMapper {

    @Mapping(target = "company", ignore = true)
    @Mapping(target = "stacks", ignore = true)
    JpaWorkplaceEntity toEntity(Workplace workplace);

    @Mapping(target = "companyId", source = "company.id")
    Workplace toDomain(JpaWorkplaceEntity entity);
	
		@Mapping(target = "companyId", source = "company.id")
		@Mapping(target = "companyName", source = "company.name")
		WorkplaceListItem toWorkplaceListItem(JpaWorkplaceEntity entity);

    List<WorkplaceListItem> toWorkplaceListItems(List<JpaWorkplaceEntity> entities);
}
