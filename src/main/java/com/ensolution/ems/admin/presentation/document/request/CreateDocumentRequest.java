package com.ensolution.ems.admin.presentation.document.request;

import com.ensolution.ems.global.common.enums.DocumentCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 파일은 {@code file} 파트로 따로 받고, 이 DTO는 메타만 담는다. */
public record CreateDocumentRequest(
	@NotBlank(message = "문서명은 필수 입력값입니다.")
	String name,

	@NotNull(message = "문서 분류는 필수 입력값입니다.")
	DocumentCategory category,

	@Size(max = 500)
	String description,

	@Size(max = 500)
	String changeNote
) {
}
