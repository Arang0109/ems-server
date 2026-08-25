package com.ensolution.ems.client_management.application.port.out;

import com.ensolution.ems.client_management.domain.Team;

import java.util.List;

public interface TeamRepository {
	Team save(Team team);
	Team findById(Long id, Long tenantId);
	List<Team> findAll(Long tenantId);
	List<Team> findAllByMemberUserId(Long userId, Long tenantId);
	boolean existsByNameAndTenantId(String name, Long tenantId);
	void deleteById(Long id, Long tenantId);
}
