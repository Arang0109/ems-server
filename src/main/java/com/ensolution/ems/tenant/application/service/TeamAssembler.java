package com.ensolution.ems.tenant.application.service;

import com.ensolution.ems.auth.application.port.in.UserQueryUseCase;
import com.ensolution.ems.auth.application.port.in.UserSummary;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.tenant.application.command.detail.TeamDetail;
import com.ensolution.ems.tenant.application.command.list_item.TeamListItem;
import com.ensolution.ems.tenant.domain.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Team 도메인에 타 모듈(auth)의 사수·부사수 이름을 결합해 조회 VO로 조립한다.
 * users는 별도 모듈이라 JPA 조인이 불가능하므로 {@link UserQueryUseCase}(인바운드 포트)로 이름을 채운다.
 * 장비 id는 검증·조립 없이 도메인 값 그대로 전달한다.
 */
@Component
@RequiredArgsConstructor
public class TeamAssembler {

	private final UserQueryUseCase userQueryUseCase;

	public TeamDetail assemble(Team team) {
		return new TeamDetail(
			team.getId(),
			team.getName(),
			team.getMentorUserId(),
			resolveName(team.getMentorUserId()),
			team.getMenteeUserId(),
			resolveName(team.getMenteeUserId()),
			team.getParticleSamplerId(),
			team.getGasSamplerId(),
			team.getPitotTubeId(),
			team.getNozzleId()
		);
	}

	public List<TeamListItem> assembleList(List<Team> teams, Long tenantId) {
		Map<Long, String> nameByUserId = userQueryUseCase.getUserList(tenantId).stream()
			.collect(Collectors.toMap(UserSummary::userId, UserSummary::name, (a, b) -> a));

		return teams.stream()
			.map(team -> new TeamListItem(
				team.getId(),
				team.getName(),
				team.getMentorUserId(),
				nameByUserId.get(team.getMentorUserId()),
				team.getMenteeUserId(),
				nameByUserId.get(team.getMenteeUserId()),
				team.getParticleSamplerId(),
				team.getGasSamplerId(),
				team.getPitotTubeId(),
				team.getNozzleId()
			))
			.toList();
	}

	private String resolveName(Long userId) {
		if (userId == null) return null;
		try {
			return userQueryUseCase.getUser(userId).name();
		} catch (CustomException e) {
			return null;
		}
	}
}
