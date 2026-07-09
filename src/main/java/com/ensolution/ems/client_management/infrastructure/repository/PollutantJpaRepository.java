package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.infrastructure.entity.PollutantEntity;
import com.ensolution.ems.global.common.enums.MeasurementField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PollutantJpaRepository extends JpaRepository<PollutantEntity, Long> {
	List<PollutantEntity> findByField(MeasurementField field);
	
	boolean existsByNameKr(String nameKr);
}
