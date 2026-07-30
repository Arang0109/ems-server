package com.ensolution.ems.storage.application.port.in;

import com.ensolution.ems.global.common.enums.DocumentCategory;

/** 문서 메타를 수정한다. 전달되지 않은 필드는 기존 값이 유지된다. */
public record UpdateDocumentCommand(
	Long documentId,
	Long tenantId,
	String name,
	DocumentCategory category,
	String description
) {
}
