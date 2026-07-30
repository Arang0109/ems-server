package com.ensolution.ems.storage.infrastructure.repository;

import com.ensolution.ems.global.common.enums.DocumentCategory;
import com.ensolution.ems.storage.infrastructure.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentJpaRepository extends JpaRepository<DocumentEntity, Long> {

	Optional<DocumentEntity> findByDocumentIdAndTenant_TenantId(Long documentId, Long tenantId);

	List<DocumentEntity> findAllByTenant_TenantIdOrderByNameAsc(Long tenantId);

	List<DocumentEntity> findAllByTenant_TenantIdAndCategoryOrderByNameAsc(Long tenantId, DocumentCategory category);

	boolean existsByNameAndTenant_TenantId(String name, Long tenantId);

	boolean existsByNameAndTenant_TenantIdAndDocumentIdNot(String name, Long tenantId, Long documentId);

	@Modifying
	@Query("delete from DocumentEntity d where d.documentId = :id and d.tenant.tenantId = :tenantId")
	int deleteByDocumentIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
}
