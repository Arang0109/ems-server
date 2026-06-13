package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.infrastructure.entity.FacilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacilityJpaRepository extends JpaRepository<FacilityEntity, Long> {
	List<FacilityEntity> findByStackId(Long stackId);
}
