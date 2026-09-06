package com.ensolution.ems.storage.application;

import com.ensolution.ems.global.common.enums.DocumentCategory;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.storage.application.command.DocumentSummary;
import com.ensolution.ems.storage.application.port.out.DocumentRepository;
import com.ensolution.ems.storage.domain.Document;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 문서 메타 검증용 인메모리 {@link DocumentRepository}.
 * <p>
 * tenant 필터와 예외 규약({@code DOCUMENT_NOT_FOUND})을 실제 어댑터 그대로 재현한다.
 */
public class FakeDocumentRepository implements DocumentRepository {

	private final List<Document> documents = new ArrayList<>();
	private final AtomicLong sequence = new AtomicLong();

	public Document given(Long tenantId, String name, int latestVersionNo) {
		Document document = Document.builder()
			.id(sequence.incrementAndGet())
			.tenantId(tenantId)
			.name(name)
			.category(DocumentCategory.ETC)
			.latestVersionNo(latestVersionNo)
			.build();
		documents.add(document);
		return document;
	}

	public Optional<Document> peek(Long id) {
		return documents.stream().filter(d -> Objects.equals(id, d.getId())).findFirst();
	}

	public int count() {
		return documents.size();
	}

	@Override
	public Document save(Document document) {
		documents.removeIf(stored -> document.getId() != null && Objects.equals(stored.getId(), document.getId()));

		Document saved = document.getId() == null
			? document.toBuilder().id(sequence.incrementAndGet()).build()
			: document;
		documents.add(saved);
		return saved;
	}

	@Override
	public Document findById(Long id, Long tenantId) {
		return documents.stream()
			.filter(d -> Objects.equals(id, d.getId()))
			.filter(d -> Objects.equals(tenantId, d.getTenantId()))
			.findFirst()
			.orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));
	}

	@Override
	public DocumentSummary findSummaryById(Long id, Long tenantId) {
		return toSummary(findById(id, tenantId));
	}

	@Override
	public List<DocumentSummary> findAllByTenantId(Long tenantId) {
		return documents.stream()
			.filter(d -> Objects.equals(tenantId, d.getTenantId()))
			.sorted(Comparator.comparing(Document::getId))
			.map(FakeDocumentRepository::toSummary)
			.toList();
	}

	@Override
	public List<DocumentSummary> findAllByTenantIdAndCategory(Long tenantId, DocumentCategory category) {
		return findAllByTenantId(tenantId).stream()
			.filter(summary -> summary.category() == category)
			.toList();
	}

	@Override
	public boolean existsByNameAndTenantId(String name, Long tenantId) {
		return documents.stream()
			.anyMatch(d -> Objects.equals(name, d.getName()) && Objects.equals(tenantId, d.getTenantId()));
	}

	@Override
	public boolean existsByNameAndTenantIdExcluding(String name, Long tenantId, Long documentId) {
		return documents.stream()
			.filter(d -> !Objects.equals(documentId, d.getId()))
			.anyMatch(d -> Objects.equals(name, d.getName()) && Objects.equals(tenantId, d.getTenantId()));
	}

	@Override
	public void deleteById(Long id, Long tenantId) {
		boolean removed = documents.removeIf(d ->
			Objects.equals(id, d.getId()) && Objects.equals(tenantId, d.getTenantId()));
		if (!removed) {
			throw new CustomException(ErrorCode.DOCUMENT_NOT_FOUND);
		}
	}

	private static DocumentSummary toSummary(Document document) {
		return new DocumentSummary(
			document.getId(),
			document.getName(),
			document.getCategory(),
			document.getDescription(),
			document.getLatestVersionNo(),
			document.getCreatedAt(),
			document.getModifiedAt()
		);
	}
}
