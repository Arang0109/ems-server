package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.infrastructure.entity.MeasurementMethodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeasurementMethodJpaRepository extends JpaRepository<MeasurementMethodEntity, Long> {
	Optional<MeasurementMethodEntity> findByMethodIdAndTenant_TenantId(Long methodId, Long tenantId);

	List<MeasurementMethodEntity> findAllByTenant_TenantIdOrderBySortOrderAscMethodIdAsc(Long tenantId);

	boolean existsByNameAndTenant_TenantId(String name, Long tenantId);

	boolean existsByNameAndTenant_TenantIdAndMethodIdNot(String name, Long tenantId, Long methodId);

	@Query("""
    select coalesce(max(m.sortOrder), 0) from MeasurementMethodEntity m
    where m.tenant.tenantId = :tenantId
""")
	Integer findMaxSortOrder(@Param("tenantId") Long tenantId);

	@Modifying
	@Query("""
    delete from MeasurementMethodEntity m
    where m.methodId = :methodId
      and m.tenant.tenantId = :tenantId
""")
	int deleteByMethodIdAndTenantId(
		@Param("methodId") Long methodId,
		@Param("tenantId") Long tenantId
	);
}
