package com.ensolution.ems.tenant.application.port.out;

import com.ensolution.ems.tenant.domain.TargetSubstance;

import java.util.List;

public interface TargetSubstanceRepository {
	TargetSubstance save(TargetSubstance targetSubstance);
	TargetSubstance findById(Long id, Long tenantId);
	List<TargetSubstance> findByPreventionId(Long preventionId, Long tenantId);
	void deleteById(Long id, Long tenantId);
}
