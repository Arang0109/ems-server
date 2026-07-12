package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.infrastructure.entity.WorkplaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkplaceJpaRepository extends JpaRepository<WorkplaceEntity, Long> {
	List<WorkplaceEntity> findByClientId(Long clientId);
	boolean existsByNameAndClientId (String name, Long clientId);
}
