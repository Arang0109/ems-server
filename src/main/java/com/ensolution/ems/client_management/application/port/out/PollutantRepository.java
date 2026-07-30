package com.ensolution.ems.client_management.application.port.out;

import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.global.common.enums.MeasurementField;

import java.util.List;

public interface PollutantRepository {
	Pollutant save(Pollutant pollutant);
	Pollutant findById(Long id, Long tenantId);
	List<Pollutant> findAll(Long tenantId);
	List<Pollutant> findByField(MeasurementField field, Long tenantId);
	void deleteById(Long id, Long tenantId);
	boolean existsByNameKrAndTenantId(String nameKr, Long tenantId);
}
