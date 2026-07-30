package com.ensolution.ems.storage.infrastructure.adapter;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.storage.application.port.in.DocumentVersionSummary;
import com.ensolution.ems.storage.application.port.out.DocumentVersionRepository;
import com.ensolution.ems.storage.domain.DocumentVersion;
import com.ensolution.ems.storage.infrastructure.entity.DocumentVersionEntity;
import com.ensolution.ems.storage.infrastructure.mapper.DocumentVersionEntityMapper;
import com.ensolution.ems.storage.infrastructure.repository.DocumentJpaRepository;
import com.ensolution.ems.storage.infrastructure.repository.DocumentVersionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
public class DocumentVersionRepositoryAdapter implements DocumentVersionRepository {

	private final DocumentVersionJpaRepository jpaDocumentVersionRepository;
	private final DocumentJpaRepository jpaDocumentRepository;
	private final DocumentVersionEntityMapper mapper;

	@Override
	public DocumentVersion save(DocumentVersion version) {
		DocumentVersionEntity entity = mapper.toEntity(version).toBuilder()
			.document(jpaDocumentRepository.getReferenceById(version.getDocumentId()))
			.build();
		return mapper.toDomain(jpaDocumentVersionRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public DocumentVersion findByDocumentIdAndVersionNo(Long documentId, int versionNo) {
		return jpaDocumentVersionRepository.findByDocument_DocumentIdAndVersionNo(documentId, versionNo)
			.map(mapper::toDomain)
			.orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_VERSION_NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<DocumentVersionSummary> findAllByDocumentId(Long documentId) {
		return mapper.toSummaries(
			jpaDocumentVersionRepository.findAllByDocument_DocumentIdOrderByVersionNoDesc(documentId)
		);
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> findStorageKeysByDocumentId(Long documentId) {
		return jpaDocumentVersionRepository.findStorageKeysByDocumentId(documentId);
	}

	@Override
	public void deleteAllByDocumentId(Long documentId) {
		jpaDocumentVersionRepository.deleteAllByDocumentId(documentId);
	}
}
