package com.ensolution.ems.tenant.application.service;

import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
import com.ensolution.ems.auth.application.port.in.UserSummary;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.tenant.application.command.create.CreateTeamCommand;
import com.ensolution.ems.tenant.application.command.detail.TeamDetail;
import com.ensolution.ems.tenant.application.command.list_item.TeamListItem;
import com.ensolution.ems.tenant.application.command.update.UpdateTeamCommand;
import com.ensolution.ems.tenant.application.port.in.TeamQueryUseCase;
import com.ensolution.ems.tenant.application.port.in.TeamSummary;
import com.ensolution.ems.tenant.application.port.out.TeamRepository;
import com.ensolution.ems.tenant.domain.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamService implements TeamQueryUseCase {

	private final TeamRepository teamRepository;
	private final TeamAssembler teamAssembler;
	private final UserQueryUseCase userQueryUseCase;

	public TeamDetail createTeam(CreateTeamCommand command) {
		if (teamRepository.existsByNameAndTenantId(command.name(), command.tenantId())) {
			throw new CustomException(ErrorCode.CONFLICT);
		}
		requireMemberInTenant(command.mentorUserId(), command.tenantId(), ErrorCode.TEAM_MENTOR_NOT_FOUND);
		requireMemberInTenant(command.menteeUserId(), command.tenantId(), ErrorCode.TEAM_MENTEE_NOT_FOUND);

		Team saved = teamRepository.save(Team.register(
			command.tenantId(), command.name(), command.mentorUserId(), command.menteeUserId(),
			command.particleSamplerId(), command.gasSamplerId(), command.pitotTubeId(), command.nozzleId()
		));
		return teamAssembler.assemble(saved);
	}

	public TeamDetail updateTeam(Long id, Long tenantId, UpdateTeamCommand command) {
		Team team = teamRepository.findById(id, tenantId);

		if (command.mentorUserId() != null) {
			requireMemberInTenant(command.mentorUserId(), tenantId, ErrorCode.TEAM_MENTOR_NOT_FOUND);
		}
		if (command.menteeUserId() != null) {
			requireMemberInTenant(command.menteeUserId(), tenantId, ErrorCode.TEAM_MENTEE_NOT_FOUND);
		}

		Team saved = teamRepository.save(team.update(
			id, command.name(), command.mentorUserId(), command.menteeUserId(),
			command.particleSamplerId(), command.gasSamplerId(), command.pitotTubeId(), command.nozzleId()
		));
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

	/**
	 * 사수·부사수 user가 존재하고 요청 tenant 소속인지 검증한다.
	 * 미존재·타 tenant 모두 동일한 도메인 NOT_FOUND로 은닉한다(멀티테넌시 규칙).
	 */
	private void requireMemberInTenant(Long userId, Long tenantId, ErrorCode notFound) {
		UserSummary user;
		try {
			user = userQueryUseCase.getUser(userId);
		} catch (CustomException e) {
			throw new CustomException(notFound);
		}
		if (!tenantId.equals(user.tenantId())) {
			throw new CustomException(notFound);
		}
	}
}
