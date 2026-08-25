package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.infrastructure.entity.FacilityEntity;
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

	/**
	 * sortOrder 는 nullable 이라 단독 정렬로는 순서가 흔들린다. facilityId 를 tie-breaker 로 동반한다.
	 * (MySQL 은 ASC 에서 NULL 을 앞에 놓으므로, 백필되지 않은 행은 목록 앞으로 온다.)
	 */
	List<FacilityEntity> findByStack_StackIdAndTenant_TenantIdOrderBySortOrderAscFacilityIdAsc(Long stackId, Long tenantId);

	/** 신규 등록 시 목록 맨 뒤에 붙이기 위한 현재 최대 순서값. 시설이 없으면 0. */
	@Query("""
    select coalesce(max(f.sortOrder), 0) from FacilityEntity f
    where f.stack.stackId = :stackId
      and f.tenant.tenantId = :tenantId
""")
	Integer findMaxSortOrder(
		@Param("stackId") Long stackId,
		@Param("tenantId") Long tenantId
	);

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
