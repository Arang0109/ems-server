package com.ensolution.ems.storage.presentation.controller;

import com.ensolution.ems.global.common.enums.DocumentCategory;
import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import com.ensolution.ems.storage.application.port.in.DocumentFile;
import com.ensolution.ems.storage.application.port.in.DocumentQueryUseCase;
import com.ensolution.ems.storage.presentation.mapper.StorageDocumentMapper;
import com.ensolution.ems.storage.presentation.response.DocumentResponse;
import com.ensolution.ems.storage.presentation.response.DocumentVersionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 문서 조회·다운로드 API. 성적서·채취기록부 양식처럼 실무자가 직접 받아야 하는 문서가 있으므로
 * 읽기는 인증된 모든 사용자에게 열려 있다. 등록·수정·삭제는 admin 모듈의 관리 API가 담당한다.
 * <p>
 * 조회 범위는 항상 요청자의 tenant로 제한된다.
 * 다운로드 엔드포인트는 바이너리를 그대로 내려보내야 하므로 {@code ApiResponse}로 감싸지 않는다.
 */
@Tag(name = "Document", description = "문서 조회·다운로드 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

	private final DocumentQueryUseCase documentQueryUseCase;

	private final StorageDocumentMapper documentMapper;

	@Operation(summary = "문서 목록 조회", description = "분류를 지정하지 않으면 전체 문서를 반환합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocuments(
		@RequestParam(required = false) DocumentCategory category,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(
			documentMapper.toResponses(documentQueryUseCase.getDocuments(principal.getTenantId(), category))
		));
	}

	@Operation(summary = "문서 단건 조회")
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<DocumentResponse>> getDocument(
		@PathVariable Long id,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(
			documentMapper.toResponse(documentQueryUseCase.getDocument(id, principal.getTenantId()))
		));
	}

	@Operation(summary = "버전 이력 조회", description = "최신 버전이 먼저 오도록 정렬해 반환합니다.")
	@GetMapping("/{id}/versions")
	public ResponseEntity<ApiResponse<List<DocumentVersionResponse>>> getVersions(
		@PathVariable Long id,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(
			documentMapper.toVersionResponses(documentQueryUseCase.getVersions(id, principal.getTenantId()))
		));
	}

	@Operation(summary = "최신본 다운로드")
	@GetMapping("/{id}/download")
	public ResponseEntity<byte[]> downloadLatest(
		@PathVariable Long id,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return toDownloadResponse(documentQueryUseCase.download(id, principal.getTenantId(), null));
	}

	@Operation(summary = "특정 버전 다운로드")
	@GetMapping("/{id}/versions/{versionNo}/download")
	public ResponseEntity<byte[]> downloadVersion(
		@PathVariable Long id,
		@PathVariable Integer versionNo,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return toDownloadResponse(documentQueryUseCase.download(id, principal.getTenantId(), versionNo));
	}

	private ResponseEntity<byte[]> toDownloadResponse(DocumentFile file) {
		MediaType contentType = file.contentType() == null
			? MediaType.APPLICATION_OCTET_STREAM
			: MediaType.parseMediaType(file.contentType());

		return ResponseEntity.ok()
			.contentType(contentType)
			.header(HttpHeaders.CONTENT_DISPOSITION, attachment(file.filename()))
			.body(file.content());
	}

	/** Content-Disposition 헤더값 생성(파일명 UTF-8 인코딩). */
	private String attachment(String filename) {
		String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
		return "attachment; filename*=UTF-8''" + encoded;
	}
}
