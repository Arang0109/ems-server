package com.ensolution.ems.client_management.application.port.out;

import com.ensolution.ems.client_management.domain.Prevention;

import java.util.List;

public interface PreventionRepository {
	Prevention save(Prevention prevention);
	Prevention findById(Long id);
	List<Prevention> findByStackId(Long stackId);
	void deleteById(Long id);
}
