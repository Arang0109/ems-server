package com.ensolution.ems.client_management.application.port.out;

import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.global.common.enums.MeasurementField;

import java.util.List;

public interface PollutantRepository {
	Pollutant save(Pollutant pollutant);
	Pollutant findById(Long id);
	List<Pollutant> findAll();
	List<Pollutant> findByField(MeasurementField field);
	void deleteById(Long id);
	boolean existsByNameKr(String nameKr);
}
