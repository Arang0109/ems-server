package com.ensolution.ems.platform.presentation.mapper;

import com.ensolution.ems.platform.application.command.ProvisionTenantCommand;
import com.ensolution.ems.platform.application.command.TenantAdminCommand;
import com.ensolution.ems.platform.application.result.TenantListItem;
import com.ensolution.ems.platform.domain.Tenant;
import com.ensolution.ems.platform.presentation.request.ProvisionTenantRequest;
import com.ensolution.ems.platform.presentation.request.TenantAdminRequest;
import com.ensolution.ems.platform.presentation.response.TenantListResponse;
import com.ensolution.ems.platform.presentation.response.TenantResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder()
)
public interface TenantMapper {

	ProvisionTenantCommand toProvisionCommand(ProvisionTenantRequest request);

	TenantAdminCommand toAdminCommand(TenantAdminRequest request);

	TenantResponse toResponse(Tenant tenant);

	TenantListResponse toListResponse(TenantListItem item);

	List<TenantListResponse> toListResponses(List<TenantListItem> items);
}
