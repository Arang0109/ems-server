package com.ensolution.ems.client_management.presentation.team.mapper;

import com.ensolution.ems.client_management.application.command.create.CreateTeamCommand;
import com.ensolution.ems.client_management.application.command.detail.TeamDetail;
import com.ensolution.ems.client_management.application.command.list_item.TeamListItem;
import com.ensolution.ems.client_management.application.command.update.UpdateTeamCommand;
import com.ensolution.ems.client_management.presentation.team.request.CreateTeamRequest;
import com.ensolution.ems.client_management.presentation.team.request.UpdateTeamRequest;
import com.ensolution.ems.client_management.presentation.team.response.TeamResponse;
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
