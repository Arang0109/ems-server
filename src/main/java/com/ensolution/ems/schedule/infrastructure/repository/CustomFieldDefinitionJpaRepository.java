package com.ensolution.ems.schedule.infrastructure.repository;

import com.ensolution.ems.schedule.infrastructure.entity.CustomFieldDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomFieldDefinitionJpaRepository extends JpaRepository<CustomFieldDefinitionEntity, Long> {

	Optional<CustomFieldDefinitionEntity> findByFieldIdAndTenantId(Long fieldId, Long tenantId);

	List<CustomFieldDefinitionEntity> findAllByTenantIdOrderBySortOrderAscFieldIdAsc(Long tenantId);

	boolean existsByKeyAndTenantId(String key, Long tenantId);

	@Query("""
    select coalesce(max(f.sortOrder), 0) from CustomFieldDefinitionEntity f
    where f.tenantId = :tenantId
""")
	Integer findMaxSortOrder(@Param("tenantId") Long tenantId);

	@Modifying
	@Query("""
    delete from CustomFieldDefinitionEntity f
    where f.fieldId = :fieldId
      and f.tenantId = :tenantId
""")
	int deleteByFieldIdAndTenantId(
		@Param("fieldId") Long fieldId,
		@Param("tenantId") Long tenantId
	);
}
