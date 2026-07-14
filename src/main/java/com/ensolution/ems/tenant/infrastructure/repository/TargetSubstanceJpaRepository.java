package com.ensolution.ems.tenant.infrastructure.repository;

import com.ensolution.ems.tenant.infrastructure.entity.TargetSubstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TargetSubstanceJpaRepository extends JpaRepository<TargetSubstanceEntity, Long> {
	Optional<TargetSubstanceEntity> findByTargetSubstanceIdAndTenant_TenantId(Long targetSubstanceId, Long tenantId);
	List<TargetSubstanceEntity> findByPrevention_PreventionIdAndTenant_TenantId(Long preventionId, Long tenantId);

	@Modifying
	@Query("""
    delete from TargetSubstanceEntity t
    where t.targetSubstanceId = :targetSubstanceId
      and t.tenant.tenantId = :tenantId
""")
	int deleteByTargetSubstanceIdAndTenantId(
		@Param("targetSubstanceId") Long targetSubstanceId,
		@Param("tenantId") Long tenantId
	);
}
