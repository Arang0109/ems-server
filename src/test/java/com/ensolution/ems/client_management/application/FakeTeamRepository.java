package com.ensolution.ems.client_management.application;

import com.ensolution.ems.client_management.application.port.out.TeamRepository;
import com.ensolution.ems.client_management.domain.Team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 팀 역조회 규칙 검증용 인메모리 {@link TeamRepository}.
 * 이 테스트들이 쓰지 않는 메서드는 의도적으로 미구현으로 두어 사용 시 즉시 드러나게 한다.
 */
public class FakeTeamRepository implements TeamRepository {

	private final List<Team> teams = new ArrayList<>();

	public void given(Long teamId, Long tenantId, String name, Long mentorUserId, Long menteeUserId) {
		teams.add(Team.builder()
			.id(teamId)
			.tenantId(tenantId)
			.name(name)
			.mentorUserId(mentorUserId)
			.menteeUserId(menteeUserId)
			.build());
	}

	/** JPA 쿼리와 동일하게 tenant 범위로 좁히고 teamId 오름차순으로 반환한다. */
	@Override
	public List<Team> findAllByMemberUserId(Long userId, Long tenantId) {
		// SQL의 `= null`은 어떤 행에도 매칭되지 않는다. 미지정 사수·부사수를 그 의미대로 재현한다.
		if (userId == null) return List.of();

		return teams.stream()
			.filter(team -> Objects.equals(tenantId, team.getTenantId()))
			.filter(team -> userId.equals(team.getMentorUserId()) || userId.equals(team.getMenteeUserId()))
			.sorted(Comparator.comparing(Team::getId))
			.toList();
	}

	@Override
	public Team save(Team team) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Team findById(Long id, Long tenantId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Team> findAll(Long tenantId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean existsByNameAndTenantId(String name, Long tenantId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void deleteById(Long id, Long tenantId) {
		throw new UnsupportedOperationException();
	}
}
