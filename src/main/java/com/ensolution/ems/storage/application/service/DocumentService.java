package com.ensolution.ems.storage.application.service;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.storage.application.port.in.*;
import com.ensolution.ems.storage.application.port.out.DocumentRepository;
import com.ensolution.ems.storage.application.port.out.DocumentVersionRepository;
import com.ensolution.ems.storage.application.port.out.FileStorageClient;
import com.ensolution.ems.storage.application.validator.DocumentValidator;
import com.ensolution.ems.storage.domain.Document;
import com.ensolution.ems.global.common.enums.DocumentCategory;
import com.ensolution.ems.storage.domain.DocumentVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 문서와 그 버전 이력을 관리한다.
 * <p>
 * 파일 쓰기는 트랜잭션과 함께 롤백되지 않으므로, 메타를 먼저 저장하고 파일 쓰기를 마지막에 둔다.
 * 그래야 파일 쓰기가 실패했을 때 트랜잭션이 롤백되며 실물 없는 레코드가 남지 않는다.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DocumentService implements DocumentCommandUseCase, DocumentQueryUseCase {

	private final DocumentRepository documentRepository;
	private final DocumentVersionRepository documentVersionRepository;
	private final FileStorageClient fileStorageClient;
	private final DocumentValidator documentValidator;

	@Override
	public Long createDocument(CreateDocumentCommand command) {
		documentValidator.requireUniqueName(command.name(), command.tenantId());

		Document document = documentRepository.save(
			Document.register(command.tenantId(), command.name(), command.category(), command.description())
		);

		storeVersion(document, command.file(), command.changeNote(), command.uploadedBy());

		return document.getId();
	}

	@Override
	public int addVersion(AddDocumentVersionCommand command) {
		Document document = documentRepository.findById(command.documentId(), command.tenantId());

		return storeVersion(document, command.file(), command.changeNote(), command.uploadedBy());
	}

	@Override
	public void updateDocument(UpdateDocumentCommand command) {
		Document document = documentRepository.findById(command.documentId(), command.tenantId());
		documentValidator.requireUniqueNameOnUpdate(command.name(), command.tenantId(), command.documentId());

		documentRepository.save(document.update(command.name(), command.category(), command.description()));
	}

	@Override
	public void deleteVersion(Long documentId, int versionNo, Long tenantId) {
		Document document = documentRepository.findById(documentId, tenantId);

		// 버전이 하나도 남지 않으면 문서가 다운로드 불가 상태로 남으므로 마지막 한 개는 남긴다.
		if (documentVersionRepository.countByDocumentId(documentId) <= 1) {
			throw new CustomException(ErrorCode.DOCUMENT_LAST_VERSION_NOT_DELETABLE);
		}

		DocumentVersion version = documentVersionRepository.findByDocumentIdAndVersionNo(documentId, versionNo);
		documentVersionRepository.deleteByDocumentIdAndVersionNo(documentId, versionNo);

		// 최신 버전을 지운 경우에만 최신 번호를 남은 버전 중 최대값으로 내린다.
		if (document.getLatestVersionNo() == versionNo) {
			documentRepository.save(
				document.withLatestVersionNo(documentVersionRepository.findMaxVersionNoByDocumentId(documentId))
			);
		}

		fileStorageClient.delete(version.getStorageKey());
	}

	@Override
	public void deleteDocument(Long documentId, Long tenantId) {
		documentRepository.findById(documentId, tenantId);

		List<String> storageKeys = documentVersionRepository.findStorageKeysByDocumentId(documentId);
		documentVersionRepository.deleteAllByDocumentId(documentId);
		documentRepository.deleteById(documentId, tenantId);

		storageKeys.forEach(fileStorageClient::delete);
	}

	@Override
	@Transactional(readOnly = true)
	public List<DocumentSummary> getDocuments(Long tenantId, DocumentCategory category) {
		return category == null
			? documentRepository.findAllByTenantId(tenantId)
			: documentRepository.findAllByTenantIdAndCategory(tenantId, category);
	}

	@Override
	@Transactional(readOnly = true)
	public DocumentSummary getDocument(Long documentId, Long tenantId) {
		return documentRepository.findSummaryById(documentId, tenantId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<DocumentVersionSummary> getVersions(Long documentId, Long tenantId) {
		documentRepository.findById(documentId, tenantId);

		return documentVersionRepository.findAllByDocumentId(documentId);
	}

	@Override
	@Transactional(readOnly = true)
	public DocumentFile download(Long documentId, Long tenantId, Integer versionNo) {
		Document document = documentRepository.findById(documentId, tenantId);

		int targetVersionNo = versionNo == null ? document.getLatestVersionNo() : versionNo;
		if (targetVersionNo <= 0) {
			throw new CustomException(ErrorCode.DOCUMENT_VERSION_NOT_FOUND);
		}

		DocumentVersion version = documentVersionRepository.findByDocumentIdAndVersionNo(documentId, targetVersionNo);

		return new DocumentFile(
			version.getOriginalFilename(),
			version.getContentType(),
			fileStorageClient.load(version.getStorageKey())
		);
	}

	/** 다음 버전을 저장하고 문서의 최신 버전 번호를 올린다. 부여된 버전 번호를 반환한다. */
	private int storeVersion(Document document, UploadedFile file, String changeNote, Long uploadedBy) {
		DocumentVersion version = documentVersionRepository.save(
			DocumentVersion.register(
				document.getTenantId(),
				document.getId(),
				document.nextVersionNo(),
				file.originalFilename(),
				file.contentType(),
				file.size(),
				fileStorageClient.provider(),
				changeNote,
				uploadedBy
			)
		);
		documentRepository.save(document.withNextVersion());

		fileStorageClient.store(version.getStorageKey(), file.content());

		return version.getVersionNo();
	}
}
