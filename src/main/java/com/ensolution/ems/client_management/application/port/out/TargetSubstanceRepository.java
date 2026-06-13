package com.ensolution.ems.client_management.application.port.out;

import com.ensolution.ems.client_management.domain.TargetSubstance;

import java.util.List;

public interface TargetSubstanceRepository {
	TargetSubstance save(TargetSubstance targetSubstance);
	TargetSubstance findById(Long id);
	List<TargetSubstance> findByPreventionId(Long preventionId);
	void deleteById(Long id);
}
