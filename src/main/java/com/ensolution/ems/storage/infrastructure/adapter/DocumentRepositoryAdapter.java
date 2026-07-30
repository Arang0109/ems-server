package com.ensolution.ems.storage.infrastructure.adapter;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.platform.infrastructure.repository.TenantJpaRepository;
import com.ensolution.ems.storage.application.port.in.DocumentSummary;
import com.ensolution.ems.storage.application.port.out.DocumentRepository;
import com.ensolution.ems.storage.domain.Document;
import com.ensolution.ems.global.common.enums.DocumentCategory;
import com.ensolution.ems.storage.infrastructure.entity.DocumentEntity;
import com.ensolution.ems.storage.infrastructure.mapper.DocumentEntityMapper;
import com.ensolution.ems.storage.infrastructure.repository.DocumentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
public class DocumentRepositoryAdapter implements DocumentRepository {

	private final DocumentJpaRepository jpaDocumentRepository;
	private final TenantJpaRepository jpaTenantRepository;
	private final DocumentEntityMapper mapper;

	@Override
	public Document save(Document document) {
		if (document.getId() != null) {
			jpaDocumentRepository.findByDocumentIdAndTenant_TenantId(document.getId(), document.getTenantId())
				.orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));
		}
		DocumentEntity entity = mapper.toEntity(document).toBuilder()
			.tenant(jpaTenantRepository.getReferenceById(document.getTenantId()))
			.build();
		return mapper.toDomain(jpaDocumentRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public Document findById(Long id, Long tenantId) {
		return jpaDocumentRepository.findByDocumentIdAndTenant_TenantId(id, tenantId)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public DocumentSummary findSummaryById(Long id, Long tenantId) {
		return jpaDocumentRepository.findByDocumentIdAndTenant_TenantId(id, tenantId)
			.map(mapper::toSummary)
			.orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<DocumentSummary> findAllByTenantId(Long tenantId) {
		return mapper.toSummaries(jpaDocumentRepository.findAllByTenant_TenantIdOrderByNameAsc(tenantId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<DocumentSummary> findAllByTenantIdAndCategory(Long tenantId, DocumentCategory category) {
		return mapper.toSummaries(
			jpaDocumentRepository.findAllByTenant_TenantIdAndCategoryOrderByNameAsc(tenantId, category)
		);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByNameAndTenantId(String name, Long tenantId) {
		return jpaDocumentRepository.existsByNameAndTenant_TenantId(name, tenantId);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByNameAndTenantIdExcluding(String name, Long tenantId, Long documentId) {
		return jpaDocumentRepository.existsByNameAndTenant_TenantIdAndDocumentIdNot(name, tenantId, documentId);
	}

	@Override
	public void deleteById(Long id, Long tenantId) {
		int deletedCount = jpaDocumentRepository.deleteByDocumentIdAndTenantId(id, tenantId);
		if (deletedCount == 0) {
			throw new CustomException(ErrorCode.DOCUMENT_NOT_FOUND);
		}
	}
}
