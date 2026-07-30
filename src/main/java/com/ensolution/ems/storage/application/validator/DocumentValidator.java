package com.ensolution.ems.storage.application.validator;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.storage.application.port.out.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 문서명은 tenant 내에서 유일해야 한다. */
@Component
@RequiredArgsConstructor
public class DocumentValidator {

	private final DocumentRepository documentRepository;

	public void requireUniqueName(String name, Long tenantId) {
		if (documentRepository.existsByNameAndTenantId(name, tenantId)) {
			throw new CustomException(ErrorCode.DOCUMENT_ALREADY_EXISTS);
		}
	}

	/** 수정 시에는 자기 자신과의 충돌을 제외한다. */
	public void requireUniqueNameOnUpdate(String name, Long tenantId, Long documentId) {
		if (name == null || name.isBlank()) {
			return;
		}
		if (documentRepository.existsByNameAndTenantIdExcluding(name, tenantId, documentId)) {
			throw new CustomException(ErrorCode.DOCUMENT_ALREADY_EXISTS);
		}
	}
}
