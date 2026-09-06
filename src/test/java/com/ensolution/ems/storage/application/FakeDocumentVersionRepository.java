package com.ensolution.ems.storage.application;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.storage.application.command.DocumentVersionSummary;
import com.ensolution.ems.storage.application.port.out.DocumentVersionRepository;
import com.ensolution.ems.storage.domain.DocumentVersion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 버전 이력 검증용 인메모리 {@link DocumentVersionRepository}.
 * <p>
 * <b>이 포트는 tenantId를 받지 않는다</b> — 모든 호출 경로가 {@code documentRepository.findById(id, tenantId)}로
 * 문서 소유권을 먼저 확인한다는 "부모 경유 격리"에 의존하기 때문이다(모듈 문서 참고).
 * 그래서 이 Fake도 tenant를 알지 못하며, 격리는 {@link FakeDocumentRepository} 쪽에서만 검증된다.
 */
public class FakeDocumentVersionRepository implements DocumentVersionRepository {

	private final List<DocumentVersion> versions = new ArrayList<>();
	private final AtomicLong sequence = new AtomicLong();

	public void given(Long documentId, int versionNo, String storageKey) {
		versions.add(DocumentVersion.builder()
			.id(sequence.incrementAndGet())
			.documentId(documentId)
			.versionNo(versionNo)
			.originalFilename("파일%d.xlsx".formatted(versionNo))
			.storageKey(storageKey)
			.size(10L)
			.contentType("application/vnd.ms-excel")
			.build());
	}

	public List<Integer> versionNosOf(Long documentId) {
		return versions.stream()
			.filter(v -> Objects.equals(documentId, v.getDocumentId()))
			.map(DocumentVersion::getVersionNo)
			.sorted()
			.toList();
	}

	@Override
	public DocumentVersion save(DocumentVersion version) {
		DocumentVersion saved = version.getId() == null
			? version.toBuilder().id(sequence.incrementAndGet()).build()
			: version;
		versions.add(saved);
		return saved;
	}

	@Override
	public DocumentVersion findByDocumentIdAndVersionNo(Long documentId, int versionNo) {
		return versions.stream()
			.filter(v -> Objects.equals(documentId, v.getDocumentId()))
			.filter(v -> v.getVersionNo() == versionNo)
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_VERSION_NOT_FOUND));
	}

	/** 어댑터와 동일하게 최신 버전이 먼저 오도록 정렬한다. */
	@Override
	public List<DocumentVersionSummary> findAllByDocumentId(Long documentId) {
		return versions.stream()
			.filter(v -> Objects.equals(documentId, v.getDocumentId()))
			.sorted(Comparator.comparingInt(DocumentVersion::getVersionNo).reversed())
			.map(v -> new DocumentVersionSummary(
				v.getVersionNo(), v.getOriginalFilename(), v.getSize(),
				v.getContentType(), v.getChangeNote(), v.getUploadedBy(), v.getCreatedAt()))
			.toList();
	}

	@Override
	public List<String> findStorageKeysByDocumentId(Long documentId) {
		return versions.stream()
			.filter(v -> Objects.equals(documentId, v.getDocumentId()))
			.map(DocumentVersion::getStorageKey)
			.toList();
	}

	@Override
	public long countByDocumentId(Long documentId) {
		return versions.stream().filter(v -> Objects.equals(documentId, v.getDocumentId())).count();
	}

	@Override
	public int findMaxVersionNoByDocumentId(Long documentId) {
		return versions.stream()
			.filter(v -> Objects.equals(documentId, v.getDocumentId()))
			.mapToInt(DocumentVersion::getVersionNo)
			.max()
			.orElse(0);
	}

	@Override
	public void deleteByDocumentIdAndVersionNo(Long documentId, int versionNo) {
		versions.removeIf(v -> Objects.equals(documentId, v.getDocumentId()) && v.getVersionNo() == versionNo);
	}

	@Override
	public void deleteAllByDocumentId(Long documentId) {
		versions.removeIf(v -> Objects.equals(documentId, v.getDocumentId()));
	}
}
