package com.ensolution.ems.tenant.infrastructure.repository;

import com.ensolution.ems.tenant.infrastructure.entity.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamJpaRepository extends JpaRepository<TeamEntity, Long> {
	Optional<TeamEntity> findByTeamIdAndTenant_TenantId(Long teamId, Long tenantId);

	List<TeamEntity> findAllByTenant_TenantId(Long tenantId);

	@Modifying
	@Query("""
    delete from TeamEntity t
    where t.teamId = :teamId
      and t.tenant.tenantId = :tenantId
""")
	int deleteByTeamIdAndTenantId(
		@Param("teamId") Long teamId,
		@Param("tenantId") Long tenantId
	);

	boolean existsByNameAndTenant_TenantId(String name, Long tenantId);
}
