package com.ensolution.ems.tenant.infrastructure.repository;

import com.ensolution.ems.tenant.infrastructure.entity.PreventionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreventionJpaRepository extends JpaRepository<PreventionEntity, Long> {
	Optional<PreventionEntity> findByPreventionIdAndTenant_TenantId(Long preventionId, Long tenantId);
	List<PreventionEntity> findByStack_StackIdAndTenant_TenantId(Long stackId, Long tenantId);

	@Modifying
	@Query("""
    delete from PreventionEntity p
    where p.preventionId = :preventionId
      and p.tenant.tenantId = :tenantId
""")
	int deleteByPreventionIdAndTenantId(
		@Param("preventionId") Long preventionId,
		@Param("tenantId") Long tenantId
	);
}
