package com.ensolution.ems.storage.presentation.response;

import com.ensolution.ems.global.common.enums.DocumentCategory;

import java.time.LocalDateTime;

public record DocumentResponse(
	Long id,
	String name,
	DocumentCategory category,
	String description,
	int latestVersionNo,
	LocalDateTime createdAt,
	LocalDateTime modifiedAt
) {
}
