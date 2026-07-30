package com.ensolution.ems.storage.application.port.out;

import com.ensolution.ems.storage.application.port.in.DocumentSummary;
import com.ensolution.ems.storage.domain.Document;
import com.ensolution.ems.global.common.enums.DocumentCategory;

import java.util.List;

public interface DocumentRepository {

	Document save(Document document);

	/** 없거나 다른 tenant의 문서면 {@code DOCUMENT_NOT_FOUND}. */
	Document findById(Long id, Long tenantId);

	/** 조회 응답용. 도메인 모델이 필요 없는 단건 조회에 쓴다. */
	DocumentSummary findSummaryById(Long id, Long tenantId);

	List<DocumentSummary> findAllByTenantId(Long tenantId);

	List<DocumentSummary> findAllByTenantIdAndCategory(Long tenantId, DocumentCategory category);

	boolean existsByNameAndTenantId(String name, Long tenantId);

	/** 수정 시 자기 자신을 제외한 동명 문서 존재 여부. */
	boolean existsByNameAndTenantIdExcluding(String name, Long tenantId, Long documentId);

	void deleteById(Long id, Long tenantId);
}
