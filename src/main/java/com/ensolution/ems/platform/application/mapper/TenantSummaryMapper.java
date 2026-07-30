package com.ensolution.ems.platform.application.mapper;

import com.ensolution.ems.platform.application.result.TenantSummary;
import com.ensolution.ems.platform.domain.Tenant;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface TenantSummaryMapper {
	@Mapping(target = "tenantId", source = "id")
	TenantSummary toSummary(Tenant domain);
}
