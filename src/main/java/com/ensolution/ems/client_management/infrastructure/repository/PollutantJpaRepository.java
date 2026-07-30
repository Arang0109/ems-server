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
	Optional<PollutantEntity> findByPollutantIdAndTenant_TenantId(Long pollutantId, Long tenantId);

	List<PollutantEntity> findAllByTenant_TenantId(Long tenantId);

	List<PollutantEntity> findByFieldAndTenant_TenantId(MeasurementField field, Long tenantId);

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

	boolean existsByNameKrAndTenant_TenantId(String nameKr, Long tenantId);
}
