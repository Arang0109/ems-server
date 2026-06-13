package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.infrastructure.entity.PreventionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreventionJpaRepository extends JpaRepository<PreventionEntity, Long> {
	List<PreventionEntity> findByStackId(Long stackId);
}
