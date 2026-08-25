package com.ensolution.ems.admin.presentation.mapper;

import com.ensolution.ems.admin.presentation.request.CreateDocumentRequest;
import com.ensolution.ems.admin.presentation.request.UpdateDocumentRequest;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.storage.application.port.in.*;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * admin의 요청 DTO와 storage가 공개한 {@code port/in} 계약 사이의 변환을 담당한다.
 * {@code MultipartFile} → {@link UploadedFile} 변환도 여기서 끝내므로
 * Spring Web 타입이 application 계층으로 넘어가지 않는다.
 */
@Mapper(componentModel = "spring", builder = @Builder)
public interface AdminDocumentMapper {

	@Mapping(target = "name", source = "request.name")
	@Mapping(target = "category", source = "request.category")
	@Mapping(target = "description", source = "request.description")
	@Mapping(target = "changeNote", source = "request.changeNote")
	@Mapping(target = "tenantId", source = "tenantId")
	@Mapping(target = "uploadedBy", source = "uploadedBy")
	@Mapping(target = "file", source = "file")
	CreateDocumentCommand toCreateCommand(
		CreateDocumentRequest request, Long tenantId, Long uploadedBy, UploadedFile file
	);

	@Mapping(target = "documentId", source = "documentId")
	@Mapping(target = "tenantId", source = "tenantId")
	@Mapping(target = "changeNote", source = "changeNote")
	@Mapping(target = "uploadedBy", source = "uploadedBy")
	@Mapping(target = "file", source = "file")
	AddDocumentVersionCommand toAddVersionCommand(
		Long documentId, Long tenantId, String changeNote, Long uploadedBy, UploadedFile file
	);

	@Mapping(target = "name", source = "request.name")
	@Mapping(target = "category", source = "request.category")
	@Mapping(target = "description", source = "request.description")
	@Mapping(target = "documentId", source = "documentId")
	@Mapping(target = "tenantId", source = "tenantId")
	UpdateDocumentCommand toUpdateCommand(UpdateDocumentRequest request, Long documentId, Long tenantId);

	default UploadedFile toUploadedFile(MultipartFile file) {
		try {
			return new UploadedFile(
				file.getOriginalFilename(),
				file.getContentType(),
				file.getSize(),
				file.getBytes()
			);
		} catch (IOException e) {
			throw new CustomException(ErrorCode.STORAGE_WRITE_FAILED);
		}
	}
}
