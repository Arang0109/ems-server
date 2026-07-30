package com.ensolution.ems.client_management.infrastructure.adapter;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.client_management.application.port.out.TeamRepository;
import com.ensolution.ems.client_management.domain.Team;
import com.ensolution.ems.client_management.infrastructure.entity.TeamEntity;
import com.ensolution.ems.client_management.infrastructure.mapper.TeamEntityMapper;
import com.ensolution.ems.client_management.infrastructure.repository.TeamJpaRepository;
import com.ensolution.ems.platform.infrastructure.repository.TenantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
public class TeamRepositoryAdapter implements TeamRepository {

	private final TeamJpaRepository jpaTeamRepository;
	private final TenantJpaRepository jpaTenantRepository;
	private final TeamEntityMapper mapper;

	@Override
	public Team save(Team team) {
		if (team.getId() != null) {
			jpaTeamRepository.findById(team.getId())
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
		}
		TeamEntity entity = mapper.toEntity(team).toBuilder()
			.tenant(jpaTenantRepository.getReferenceById(team.getTenantId()))
			.build();
		return mapper.toDomain(jpaTeamRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public Team findById(Long id, Long tenantId) {
		return jpaTeamRepository.findByTeamIdAndTenant_TenantId(id, tenantId)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Team> findAll(Long tenantId) {
		return mapper.toDomainList(jpaTeamRepository.findAllByTenant_TenantId(tenantId));
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByNameAndTenantId(String name, Long tenantId) {
		return jpaTeamRepository.existsByNameAndTenant_TenantId(name, tenantId);
	}

	@Override
	public void deleteById(Long id, Long tenantId) {
		int deletedCount = jpaTeamRepository.deleteByTeamIdAndTenantId(id, tenantId);

		if (deletedCount == 0) {
			throw new CustomException(ErrorCode.NOT_FOUND);
		}
	}
}
