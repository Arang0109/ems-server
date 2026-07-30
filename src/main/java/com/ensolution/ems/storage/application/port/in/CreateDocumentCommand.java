package com.ensolution.ems.storage.application.port.in;

import com.ensolution.ems.global.common.enums.DocumentCategory;

/** 문서를 새로 등록한다. 첨부된 파일이 곧 1번 버전이 된다. */
public record CreateDocumentCommand(
	Long tenantId,
	String name,
	DocumentCategory category,
	String description,
	String changeNote,
	Long uploadedBy,
	UploadedFile file
) {
}
