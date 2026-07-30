package com.ensolution.ems.storage.application.port.in;

/** 문서 쓰기 유스케이스. 다른 모듈은 이 계약으로만 storage에 접근한다. */
public interface DocumentCommandUseCase {

	/** 문서를 등록하고 첨부 파일을 1번 버전으로 저장한다. 생성된 문서 id를 반환한다. */
	Long createDocument(CreateDocumentCommand command);

	/** 새 버전을 추가하고 부여된 버전 번호를 반환한다. */
	int addVersion(AddDocumentVersionCommand command);

	void updateDocument(UpdateDocumentCommand command);

	/** 문서와 그에 속한 모든 버전을 파일까지 함께 삭제한다. */
	void deleteDocument(Long documentId, Long tenantId);
}
