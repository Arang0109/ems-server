package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.application.port.in.StackMeasurementItemSummary;
import com.ensolution.ems.client_management.infrastructure.entity.StackPollutantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StackPollutantJpaRepository extends JpaRepository<StackPollutantEntity, Long> {

	@Query("SELECT sp FROM StackPollutantEntity sp JOIN FETCH sp.pollutant p LEFT JOIN FETCH p.catalog WHERE sp.stack.id = :stackId AND sp.tenant.tenantId = :tenantId")
	List<StackPollutantEntity> findByStackId(@Param("stackId") Long stackId, @Param("tenantId") Long tenantId);

	/**
	 * 측정항목을 사업장·측정시설 이름과 함께 평면 목록으로 조회한다(주기 이행 현황판의 행 축).
	 * 시설을 순회하며 트리를 반복 조회하지 않도록 조인 한 번으로 끝낸다.
	 * {@code workplaceId}·{@code stackId}는 선택 필터이며 null이면 조건에서 빠진다.
	 */
	@Query("""
		select new com.ensolution.ems.client_management.application.port.in.StackMeasurementItemSummary(
			w.workplaceId, w.name, s.stackId, s.name, s.field,
			sp.stackPollutantId, p.pollutantId, c.code, p.nameKr, sp.cycle, sp.allowance)
		from StackPollutantEntity sp
			join sp.stack s
			join s.workplace w
			join sp.pollutant p
			left join p.catalog c
		where sp.tenant.tenantId = :tenantId
		  and (:workplaceId is null or w.workplaceId = :workplaceId)
		  and (:stackId is null or s.stackId = :stackId)
		order by w.name asc, s.name asc, c.sortOrder asc, p.nameKr asc
	""")
	List<StackMeasurementItemSummary> findMeasurementItems(
		@Param("tenantId") Long tenantId,
		@Param("workplaceId") Long workplaceId,
		@Param("stackId") Long stackId
	);

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

	Optional<StackPollutantEntity> findByStackPollutantIdAndTenant_TenantId(Long id, Long tenantId);
}
