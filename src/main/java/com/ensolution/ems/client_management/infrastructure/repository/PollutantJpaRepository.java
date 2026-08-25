package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.infrastructure.entity.PollutantEntity;
import com.ensolution.ems.global.common.enums.MeasurementField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PollutantJpaRepository extends JpaRepository<PollutantEntity, Long> {

	@Query("""
    select p from PollutantEntity p
    join fetch p.catalog
    where p.pollutantId = :pollutantId
      and p.tenant.tenantId = :tenantId
""")
	Optional<PollutantEntity> findByPollutantIdAndTenant_TenantId(
		@Param("pollutantId") Long pollutantId,
		@Param("tenantId") Long tenantId
	);

	/** 정렬 근거인 sortOrder는 카탈로그가 소유하므로 이미 조인해 온 카탈로그로 정렬한다. */
	@Query("""
    select p from PollutantEntity p
    join fetch p.catalog c
    where p.tenant.tenantId = :tenantId
    order by c.sortOrder asc, p.nameKr asc
""")
	List<PollutantEntity> findAllByTenant_TenantId(@Param("tenantId") Long tenantId);

	/** 측정분야 필터. 측정분야는 카탈로그가 단일 진실 소스이므로 카탈로그 기준으로만 거른다. */
	@Query("""
    select p from PollutantEntity p
    join fetch p.catalog c
    where p.tenant.tenantId = :tenantId
      and c.field = :field
    order by c.sortOrder asc, p.nameKr asc
""")
	List<PollutantEntity> findByFieldAndTenant_TenantId(
		@Param("field") MeasurementField field,
		@Param("tenantId") Long tenantId
	);

	@Query("""
    select p from PollutantEntity p
    join fetch p.catalog c
    where p.tenant.tenantId = :tenantId
      and c.catalogId = :catalogId
""")
	Optional<PollutantEntity> findByCatalogIdAndTenantId(
		@Param("catalogId") Long catalogId,
		@Param("tenantId") Long tenantId
	);

	boolean existsByCatalog_CatalogId(Long catalogId);

	@Modifying
	@Query("""
    delete from PollutantEntity p
    where p.pollutantId = :pollutantId
      and p.tenant.tenantId = :tenantId
""")
	int deleteByPollutantIdAndTenantId(
		@Param("pollutantId") Long pollutantId,
		@Param("tenantId") Long tenantId
	);
}
