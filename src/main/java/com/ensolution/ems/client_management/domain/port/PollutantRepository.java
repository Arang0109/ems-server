package com.ensolution.ems.client_management.domain.port;

import com.ensolution.ems.client_management.domain.Pollutant;

import java.util.List;

public interface PollutantRepository {
	Pollutant save(Pollutant pollutant);
	Pollutant findById(Long id);
	List<Pollutant> findAll();
	void deleteById(Long id);
	boolean existsByNameKr(String nameKr);
}
