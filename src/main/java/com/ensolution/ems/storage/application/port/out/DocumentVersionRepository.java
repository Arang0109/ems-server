package com.ensolution.ems.storage.application.port.out;

import com.ensolution.ems.storage.application.port.in.DocumentVersionSummary;
import com.ensolution.ems.storage.domain.DocumentVersion;

import java.util.List;

/**
 * 버전은 항상 소유권이 확인된 {@code documentId} 아래에서만 조회된다.
 * 부모가 이미 tenant에 종속되므로 tenant 파라미터를 따로 두지 않는다.
 */
public interface DocumentVersionRepository {

	DocumentVersion save(DocumentVersion version);

	/** 없으면 {@code DOCUMENT_VERSION_NOT_FOUND}. */
	DocumentVersion findByDocumentIdAndVersionNo(Long documentId, int versionNo);

	/** 최신 버전이 먼저 오도록 정렬해 반환한다. */
	List<DocumentVersionSummary> findAllByDocumentId(Long documentId);

	/** 문서 삭제 시 실물 파일을 지우기 위한 저장소 키 목록. */
	List<String> findStorageKeysByDocumentId(Long documentId);

	long countByDocumentId(Long documentId);

	/** 남아 있는 버전 중 가장 큰 번호. 버전이 없으면 0. */
	int findMaxVersionNoByDocumentId(Long documentId);

	void deleteByDocumentIdAndVersionNo(Long documentId, int versionNo);

	void deleteAllByDocumentId(Long documentId);
}
