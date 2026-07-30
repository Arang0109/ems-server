package com.ensolution.ems.client_management.infrastructure.repository;

import com.ensolution.ems.client_management.infrastructure.entity.StackEntity;
import com.ensolution.ems.global.common.enums.MeasurementField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StackJpaRepository extends JpaRepository<StackEntity, Long> {
    List<StackEntity> findByWorkplace_WorkplaceId(Long workplaceId);
	boolean existsByNameAndWorkplace_WorkplaceIdAndField(String name, Long workplaceId, MeasurementField field);

	Optional<StackEntity> findByStackIdAndTenant_TenantId(Long stackId, Long tenantId);
	List<StackEntity> findAllByTenant_TenantId(Long tenantId);
	List<StackEntity> findByWorkplace_WorkplaceIdAndTenant_TenantId(Long workplaceId, Long tenantId);

	@Modifying
	@Query("delete from StackEntity s where s.stackId = :id and s.tenant.tenantId = :tenantId")
	int deleteByStackIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

	@Query("SELECT s FROM StackEntity s JOIN FETCH s.workplace w WHERE w.id IN :workplaceIds AND s.tenant.tenantId = :tenantId")
	List<StackEntity> findByWorkplaceIds(@Param("workplaceIds") List<Long> workplaceIds, @Param("tenantId") Long tenantId);
}
