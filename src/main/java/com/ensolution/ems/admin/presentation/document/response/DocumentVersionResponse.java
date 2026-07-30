package com.ensolution.ems.admin.presentation.document.response;

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
