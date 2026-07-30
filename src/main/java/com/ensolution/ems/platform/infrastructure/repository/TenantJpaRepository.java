package com.ensolution.ems.platform.infrastructure.repository;

import com.ensolution.ems.platform.infrastructure.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantJpaRepository extends JpaRepository<TenantEntity, Long> {
	Optional<TenantEntity> findByTenantId(Long tenantId);

	Optional<TenantEntity> findByBizNumber(String bizNumber);

	boolean existsByBizNumber(String bizNumber);
}
