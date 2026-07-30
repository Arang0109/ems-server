package com.ensolution.ems.storage.application.port.in;

import java.time.LocalDateTime;

/** 문서 버전 이력 한 건. 저장소 키는 내부 정보이므로 노출하지 않는다. */
public record DocumentVersionSummary(
	int versionNo,
	String originalFilename,
	Long size,
	String contentType,
	String changeNote,
	Long uploadedBy,
	LocalDateTime createdAt
) {
}
