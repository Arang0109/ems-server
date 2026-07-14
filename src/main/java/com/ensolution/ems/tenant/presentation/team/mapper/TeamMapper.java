package com.ensolution.ems.tenant.presentation.team.mapper;

import com.ensolution.ems.tenant.application.command.create.CreateTeamCommand;
import com.ensolution.ems.tenant.application.command.detail.TeamDetail;
import com.ensolution.ems.tenant.application.command.list_item.TeamListItem;
import com.ensolution.ems.tenant.application.command.update.UpdateTeamCommand;
import com.ensolution.ems.tenant.presentation.team.request.CreateTeamRequest;
import com.ensolution.ems.tenant.presentation.team.request.UpdateTeamRequest;
import com.ensolution.ems.tenant.presentation.team.response.TeamResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface TeamMapper {
	@Mapping(target = "tenantId", source = "tenantId")
	CreateTeamCommand toCreateCommand(CreateTeamRequest request, Long tenantId);

	UpdateTeamCommand toUpdateCommand(UpdateTeamRequest request);

	TeamResponse toResponse(TeamDetail detail);

	List<TeamResponse> toListResponses(List<TeamListItem> items);
}
