package com.ensolution.ems.storage.application.port.in;

import com.ensolution.ems.global.common.enums.DocumentCategory;

import java.util.List;

/** 문서 조회 유스케이스. 다른 모듈은 이 계약으로만 storage에 접근한다. */
public interface DocumentQueryUseCase {

	/** {@code category}가 null이면 tenant의 전체 문서를 반환한다. */
	List<DocumentSummary> getDocuments(Long tenantId, DocumentCategory category);

	DocumentSummary getDocument(Long documentId, Long tenantId);

	List<DocumentVersionSummary> getVersions(Long documentId, Long tenantId);

	/** {@code versionNo}가 null이면 최신 버전을 내려준다. */
	DocumentFile download(Long documentId, Long tenantId, Integer versionNo);
}
