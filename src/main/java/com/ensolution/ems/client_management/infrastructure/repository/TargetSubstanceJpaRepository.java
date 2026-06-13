package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.infrastructure.entity.TargetSubstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TargetSubstanceJpaRepository extends JpaRepository<TargetSubstanceEntity, Long> {
	List<TargetSubstanceEntity> findByPreventionId(Long preventionId);
}
