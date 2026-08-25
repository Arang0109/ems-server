package com.ensolution.ems.admin.presentation.request;

import com.ensolution.ems.global.common.enums.DocumentCategory;
import jakarta.validation.constraints.Size;

/** 전달하지 않은 필드는 기존 값이 유지된다. */
public record UpdateDocumentRequest(
	String name,

	DocumentCategory category,

	@Size(max = 500)
	String description
) {
}
