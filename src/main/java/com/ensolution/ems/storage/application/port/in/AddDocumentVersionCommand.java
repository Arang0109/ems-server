package com.ensolution.ems.storage.application.port.in;

/** 기존 문서에 새 버전을 추가한다. */
public record AddDocumentVersionCommand(
	Long documentId,
	Long tenantId,
	String changeNote,
	Long uploadedBy,
	UploadedFile file
) {
}
