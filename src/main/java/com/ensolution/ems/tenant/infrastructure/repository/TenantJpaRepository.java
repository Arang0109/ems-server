package com.ensolution.ems.tenant.infrastructure.repository;

import com.ensolution.ems.tenant.infrastructure.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantJpaRepository extends JpaRepository<TenantEntity, Long> {
	Optional<TenantEntity> findByTenantId(Long tenantId);

	boolean existsByBizNumber(String bizNumber);
}
