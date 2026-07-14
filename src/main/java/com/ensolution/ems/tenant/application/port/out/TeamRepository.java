package com.ensolution.ems.tenant.application.port.out;

import com.ensolution.ems.tenant.domain.Team;

import java.util.List;

public interface TeamRepository {
	Team save(Team team);
	Team findById(Long id, Long tenantId);
	List<Team> findAll(Long tenantId);
	boolean existsByNameAndTenantId(String name, Long tenantId);
	void deleteById(Long id, Long tenantId);
}
