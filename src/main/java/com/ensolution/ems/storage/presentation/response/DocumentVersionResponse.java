package com.ensolution.ems.storage.presentation.response;

import java.time.LocalDateTime;

public record DocumentVersionResponse(
	int versionNo,
	String originalFilename,
	Long size,
	String contentType,
	String changeNote,
	Long uploadedBy,
	LocalDateTime createdAt
) {
}
