package com.ensolution.ems.tenant.infrastructure.repository;

import com.ensolution.ems.tenant.infrastructure.entity.FacilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacilityJpaRepository extends JpaRepository<FacilityEntity, Long> {
	Optional<FacilityEntity> findByFacilityIdAndTenant_TenantId(Long facilityId, Long tenantId);
	List<FacilityEntity> findByStack_StackIdAndTenant_TenantId(Long stackId, Long tenantId);

	@Modifying
	@Query("""
    delete from FacilityEntity f
    where f.facilityId = :facilityId
      and f.tenant.tenantId = :tenantId
""")
	int deleteByFacilityIdAndTenantId(
		@Param("facilityId") Long facilityId,
		@Param("tenantId") Long tenantId
	);
}
