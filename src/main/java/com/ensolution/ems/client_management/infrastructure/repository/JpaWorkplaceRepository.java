package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.infrastructure.entity.JpaWorkplaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaWorkplaceRepository extends JpaRepository<JpaWorkplaceEntity, Long> {
    List<JpaWorkplaceEntity> findByCompanyId(Long companyId);
}
