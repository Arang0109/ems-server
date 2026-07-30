package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.infrastructure.entity.StackPollutantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StackPollutantJpaRepository extends JpaRepository<StackPollutantEntity, Long> {

	@Query("SELECT sp FROM StackPollutantEntity sp JOIN FETCH sp.pollutant WHERE sp.stack.id = :stackId AND sp.tenant.tenantId = :tenantId")
	List<StackPollutantEntity> findByStackId(@Param("stackId") Long stackId, @Param("tenantId") Long tenantId);

	@Modifying
	@Query("""
    delete from StackPollutantEntity sp
    where sp.stackPollutantId = :id
      and sp.tenant.tenantId = :tenantId
""")
	int deleteByStackPollutantIdAndTenantId(
		@Param("id") Long id,
		@Param("tenantId") Long tenantId
	);

	boolean existsByStack_StackIdAndPollutant_PollutantId(Long stackId, Long pollutantId);
}
