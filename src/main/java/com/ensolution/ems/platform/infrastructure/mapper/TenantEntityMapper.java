package com.ensolution.ems.platform.infrastructure.mapper;

import com.ensolution.ems.platform.application.result.TenantListItem;
import com.ensolution.ems.platform.domain.Tenant;
import com.ensolution.ems.platform.infrastructure.entity.TenantEntity;
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
public interface TenantEntityMapper {

	@Mapping(target = "tenantId", source = "id")
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	TenantEntity toEntity(Tenant tenant);

	@Mapping(target = "id", source = "tenantId")
	Tenant toDomain(TenantEntity entity);

	@Mapping(target = "id", source = "tenantId")
	TenantListItem toListItem(TenantEntity entity);

	List<TenantListItem> toListItems(List<TenantEntity> entities);
}
