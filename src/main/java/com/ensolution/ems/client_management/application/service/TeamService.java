package com.ensolution.ems.client_management.application.service;

import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.client_management.application.command.create.CreateTeamCommand;
import com.ensolution.ems.client_management.application.command.detail.TeamDetail;
import com.ensolution.ems.client_management.application.command.list_item.TeamListItem;
import com.ensolution.ems.client_management.application.command.update.UpdateTeamCommand;
import com.ensolution.ems.client_management.application.port.in.TeamQueryUseCase;
import com.ensolution.ems.client_management.application.port.in.TeamSummary;
import com.ensolution.ems.client_management.application.port.in.UserTeamSummary;
import com.ensolution.ems.client_management.application.port.out.TeamRepository;
import com.ensolution.ems.client_management.application.validator.TeamValidator;
import com.ensolution.ems.client_management.domain.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamService implements TeamQueryUseCase {

	private final TeamRepository teamRepository;
	private final TeamValidator teamValidator;
	private final TeamAssembler teamAssembler;

	public TeamDetail createTeam(CreateTeamCommand command) {
		teamValidator.requireUniqueName(command.name(), command.tenantId());
		teamValidator.requireDistinctMembers(command.mentorUserId(), command.menteeUserId());
		teamValidator.requireMemberInTenant(command.mentorUserId(), command.tenantId(), ErrorCode.TEAM_MENTOR_NOT_FOUND);
		teamValidator.requireMemberInTenant(command.menteeUserId(), command.tenantId(), ErrorCode.TEAM_MENTEE_NOT_FOUND);
		teamValidator.requireNotAssignedToOtherTeam(command.mentorUserId(), command.tenantId(), null);
		teamValidator.requireNotAssignedToOtherTeam(command.menteeUserId(), command.tenantId(), null);

		Team saved = teamRepository.save(Team.register(
			command.tenantId(), command.name(), command.mentorUserId(), command.menteeUserId(),
			command.particleSamplerId(), command.gasSamplerId(), command.pitotTubeId(), command.nozzleId()
		));
		return teamAssembler.assemble(saved);
	}

	public TeamDetail updateTeam(Long id, Long tenantId, UpdateTeamCommand command) {
		Team team = teamRepository.findById(id, tenantId);

		if (command.mentorUserId() != null) {
			teamValidator.requireMemberInTenant(command.mentorUserId(), tenantId, ErrorCode.TEAM_MENTOR_NOT_FOUND);
			teamValidator.requireNotAssignedToOtherTeam(command.mentorUserId(), tenantId, id);
		}
		if (command.menteeUserId() != null) {
			teamValidator.requireMemberInTenant(command.menteeUserId(), tenantId, ErrorCode.TEAM_MENTEE_NOT_FOUND);
			teamValidator.requireNotAssignedToOtherTeam(command.menteeUserId(), tenantId, id);
		}

		Team updated = team.update(
			id, command.name(), command.mentorUserId(), command.menteeUserId(),
			command.particleSamplerId(), command.gasSamplerId(), command.pitotTubeId(), command.nozzleId()
		);
		// 부분 수정이므로 병합 후 값으로 검증한다(사수만 바꿔 기존 부사수와 겹치는 경우)
		teamValidator.requireDistinctMembers(updated.getMentorUserId(), updated.getMenteeUserId());

		Team saved = teamRepository.save(updated);
		return teamAssembler.assemble(saved);
	}

	public void deleteTeam(Long id, Long tenantId) {
		teamRepository.deleteById(id, tenantId);
	}

	@Transactional(readOnly = true)
	public List<TeamListItem> getTeamList(Long tenantId) {
		return teamAssembler.assembleList(teamRepository.findAll(tenantId), tenantId);
	}

	@Transactional(readOnly = true)
	public TeamDetail getTeam(Long id, Long tenantId) {
		return teamAssembler.assemble(teamRepository.findById(id, tenantId));
	}

	@Override
	@Transactional(readOnly = true)
	public UserTeamSummary getUserTeamSummary(Long userId, Long tenantId) {
		return teamRepository.findAllByMemberUserId(userId, tenantId).stream()
			.findFirst()
			.map(team -> new UserTeamSummary(team.getId(), team.getName()))
			.orElse(null);
	}

	@Override
	@Transactional(readOnly = true)
	public TeamSummary getTeamSummary(Long teamId, Long tenantId) {
		TeamDetail detail = teamAssembler.assemble(teamRepository.findById(teamId, tenantId));
		return new TeamSummary(
			detail.id(),
			detail.name(),
			detail.mentorUserId(),
			detail.mentorName(),
			detail.menteeUserId(),
			detail.menteeName(),
			detail.particleSamplerId(),
			detail.gasSamplerId(),
			detail.pitotTubeId(),
			detail.nozzleId()
		);
	}
}
