package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.infrastructure.entity.PollutantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PollutantJpaRepository extends JpaRepository<PollutantEntity, Long> {
	boolean existsByNameKr(String nameKr);
}
