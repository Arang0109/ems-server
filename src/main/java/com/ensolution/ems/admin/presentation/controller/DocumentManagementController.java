package com.ensolution.ems.admin.presentation.controller;

import com.ensolution.ems.admin.presentation.mapper.AdminDocumentMapper;
import com.ensolution.ems.admin.presentation.request.CreateDocumentRequest;
import com.ensolution.ems.admin.presentation.request.UpdateDocumentRequest;
import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import com.ensolution.ems.storage.application.port.in.DocumentCommandUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 관리자 문서 보관·버전 관리 API. 실제 저장과 버전 부여는 storage 모듈이 담당하며,
 * 이 컨트롤러는 storage가 공개한 {@code port/in} 계약만 호출한다.
 * <p>
 * 조회·다운로드는 실무자도 사용해야 하므로 storage 모듈의 {@code /api/documents}가 담당하고,
 * 여기에는 ADMIN 전용 쓰기 기능만 둔다.
 */
@Tag(name = "Admin Document Management", description = "문서 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
public class DocumentManagementController {

	private final DocumentCommandUseCase documentCommandUseCase;

	private final AdminDocumentMapper documentMapper;

	@Operation(summary = "문서 등록", description = "문서를 등록하고 첨부한 파일을 1번 버전으로 저장합니다.")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<Long>> createDocument(
		@Valid @ModelAttribute CreateDocumentRequest request,
		@RequestPart("file") MultipartFile file,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		Long documentId = documentCommandUseCase.createDocument(
			documentMapper.toCreateCommand(
				request, principal.getTenantId(), principal.getUserId(), documentMapper.toUploadedFile(file)
			)
		);
		return ResponseEntity.ok().body(ApiResponse.success(documentId));
	}

	@Operation(summary = "새 버전 업로드", description = "기존 문서에 새 버전을 추가하고 부여된 버전 번호를 반환합니다.")
	@PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<Integer>> addVersion(
		@PathVariable Long id,
		@RequestParam(required = false) String changeNote,
		@RequestPart("file") MultipartFile file,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		int versionNo = documentCommandUseCase.addVersion(
			documentMapper.toAddVersionCommand(
				id, principal.getTenantId(), changeNote, principal.getUserId(), documentMapper.toUploadedFile(file)
			)
		);
		return ResponseEntity.ok().body(ApiResponse.success(versionNo));
	}

	@Operation(summary = "문서 메타 수정", description = "전달하지 않은 필드는 기존 값이 유지됩니다. 파일은 새 버전 업로드로 교체합니다.")
	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> updateDocument(
		@PathVariable Long id,
		@Valid @RequestBody UpdateDocumentRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		documentCommandUseCase.updateDocument(documentMapper.toUpdateCommand(request, id, principal.getTenantId()));
		return ResponseEntity.ok().body(ApiResponse.success());
	}

	@Operation(summary = "문서 삭제", description = "문서와 그에 속한 모든 버전을 파일까지 함께 삭제합니다.")
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteDocument(
		@PathVariable Long id,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		documentCommandUseCase.deleteDocument(id, principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success());
	}
}
