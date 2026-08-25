package com.ensolution.ems.storage.presentation.mapper;

import com.ensolution.ems.storage.application.port.in.DocumentSummary;
import com.ensolution.ems.storage.application.port.in.DocumentVersionSummary;
import com.ensolution.ems.storage.presentation.response.DocumentResponse;
import com.ensolution.ems.storage.presentation.response.DocumentVersionResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

/** 문서 조회 결과 VO를 응답 DTO로 변환한다. */
@Mapper(componentModel = "spring", builder = @Builder)
public interface StorageDocumentMapper {

	DocumentResponse toResponse(DocumentSummary summary);

	List<DocumentResponse> toResponses(List<DocumentSummary> summaries);

	DocumentVersionResponse toVersionResponse(DocumentVersionSummary summary);

	List<DocumentVersionResponse> toVersionResponses(List<DocumentVersionSummary> summaries);
}
