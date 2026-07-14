package com.ensolution.ems.tenant.application.port.out;

import com.ensolution.ems.tenant.domain.Prevention;

import java.util.List;

public interface PreventionRepository {
	Prevention save(Prevention prevention);
	Prevention findById(Long id, Long tenantId);
	List<Prevention> findByStackId(Long stackId, Long tenantId);
	void deleteById(Long id, Long tenantId);
}
