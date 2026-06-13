package com.ensolution.ems.client_management.application.port.out;

import com.ensolution.ems.client_management.domain.Facility;

import java.util.List;

public interface FacilityRepository {
	Facility save(Facility facility);
	Facility findById(Long id);
	List<Facility> findByStackId(Long stackId);
	void deleteById(Long id);
}
