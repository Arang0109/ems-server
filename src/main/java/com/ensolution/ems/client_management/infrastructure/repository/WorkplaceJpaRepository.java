package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.infrastructure.entity.WorkplaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkplaceJpaRepository extends JpaRepository<WorkplaceEntity, Long> {
	List<WorkplaceEntity> findByClient_ClientId(Long clientId);
	boolean existsByNameAndClient_ClientId (String name, Long clientId);

	Optional<WorkplaceEntity> findByWorkplaceIdAndTenant_TenantId(Long workplaceId, Long tenantId);
	List<WorkplaceEntity> findByClient_ClientIdAndTenant_TenantId(Long clientId, Long tenantId);
	List<WorkplaceEntity> findAllByTenant_TenantId(Long tenantId);

	@Modifying
	@Query("delete from WorkplaceEntity w where w.workplaceId = :id and w.tenant.tenantId = :tenantId")
	int deleteByWorkplaceIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
}
