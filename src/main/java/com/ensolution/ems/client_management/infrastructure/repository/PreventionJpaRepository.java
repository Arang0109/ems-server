package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.infrastructure.entity.PreventionEntity;
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

	/**
	 * sortOrder 는 nullable 이라 단독 정렬로는 순서가 흔들린다. preventionId 를 tie-breaker 로 동반한다.
	 * (MySQL 은 ASC 에서 NULL 을 앞에 놓으므로, 백필되지 않은 행은 목록 앞으로 온다.)
	 */
	List<PreventionEntity> findByStack_StackIdAndTenant_TenantIdOrderBySortOrderAscPreventionIdAsc(Long stackId, Long tenantId);

	/** 신규 등록 시 목록 맨 뒤에 붙이기 위한 현재 최대 순서값. 시설이 없으면 0. */
	@Query("""
    select coalesce(max(p.sortOrder), 0) from PreventionEntity p
    where p.stack.stackId = :stackId
      and p.tenant.tenantId = :tenantId
""")
	Integer findMaxSortOrder(
		@Param("stackId") Long stackId,
		@Param("tenantId") Long tenantId
	);

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
