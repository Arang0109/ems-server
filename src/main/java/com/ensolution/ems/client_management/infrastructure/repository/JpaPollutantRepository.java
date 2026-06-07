package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.infrastructure.entity.JpaPollutantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaPollutantRepository extends JpaRepository<JpaPollutantEntity, Long> {
	boolean existsByNameKr(String nameKr);
}
