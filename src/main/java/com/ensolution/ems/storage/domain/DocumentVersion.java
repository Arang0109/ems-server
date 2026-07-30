package com.ensolution.ems.storage.domain;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link Document}의 개별 버전. 한 번 등록되면 변경되지 않으므로 {@code update()}를 두지 않는다.
 * <p>
 * 실제 저장 경로가 되는 {@code storageKey}는 서버가 생성하며 원본 파일명을 포함하지 않는다.
 * 원본 파일명은 이 레코드에만 남아 다운로드 시 되돌려주는 용도로 쓰인다.
 * 사용자가 준 문자열이 경로에 섞이지 않으므로 경로 순회·동명 파일 덮어쓰기가 원천 차단된다.
 */
@Builder(toBuilder = true)
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DocumentVersion {

	/** 확장자로 인정할 패턴. 이에 맞지 않으면 확장자 없이 저장한다. */
	private static final Pattern SAFE_EXTENSION = Pattern.compile("\\.[A-Za-z0-9]{1,10}$");

	private Long id;
	private Long documentId;

	private int versionNo;

	private String originalFilename;
	/** 저장소 내 위치를 가리키는 키. {@code {tenantId}/{documentId}/{versionNo}/{UUID}{확장자}} */
	private String storageKey;
	private Long size;
	private String contentType;
	private StorageProvider provider;

	private String changeNote;
	private Long uploadedBy;

	private LocalDateTime createdAt;

	public static DocumentVersion register(
		Long tenantId,
		Long documentId,
		int versionNo,
		String originalFilename,
		String contentType,
		Long size,
		StorageProvider provider,
		String changeNote,
		Long uploadedBy
	) {
		requireStorable(originalFilename, size);

		return DocumentVersion.builder()
			.documentId(documentId)
			.versionNo(versionNo)
			.originalFilename(originalFilename)
			.storageKey(generateStorageKey(tenantId, documentId, versionNo, originalFilename))
			.contentType(contentType)
			.size(size)
			.provider(provider)
			.changeNote(changeNote)
			.uploadedBy(uploadedBy)
			.build();
	}

	private static void requireStorable(String originalFilename, Long size) {
		if (originalFilename == null || originalFilename.isBlank()) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		if (size == null || size <= 0) {
			throw new CustomException(ErrorCode.DOCUMENT_FILE_EMPTY);
		}
	}

	private static String generateStorageKey(Long tenantId, Long documentId, int versionNo, String originalFilename) {
		return "%d/%d/%d/%s%s".formatted(
			tenantId, documentId, versionNo, UUID.randomUUID(), extensionOf(originalFilename)
		);
	}

	private static String extensionOf(String originalFilename) {
		Matcher matcher = SAFE_EXTENSION.matcher(originalFilename);
		return matcher.find() ? matcher.group().toLowerCase() : "";
	}
}
