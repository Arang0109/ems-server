package com.ensolution.ems.tenant.presentation.prevention.controller;

import com.ensolution.ems.tenant.application.service.PreventionService;
import com.ensolution.ems.tenant.domain.Prevention;
import com.ensolution.ems.tenant.presentation.prevention.mapper.PreventionMapper;
import com.ensolution.ems.tenant.presentation.prevention.request.CreatePreventionRequest;
import com.ensolution.ems.tenant.presentation.prevention.request.UpdatePreventionRequest;
import com.ensolution.ems.tenant.presentation.prevention.response.PreventionResponse;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.ensolution.ems.global.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Prevention", description = "방지설비 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/preventions")
@RequiredArgsConstructor
public class PreventionController {

	private final PreventionService preventionService;
	private final PreventionMapper mapper;

	@Operation(summary = "방지설비 등록")
	@PostMapping
	public ResponseEntity<ApiResponse<PreventionResponse>> createPrevention(
		@Valid @RequestBody CreatePreventionRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		Prevention prevention = preventionService.createPrevention(mapper.toCreateCommand(request, principal.getTenantId()));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(prevention)));
	}

	@Operation(summary = "방지설비 목록 조회", description = "stackId 쿼리 파라미터로 측정시설을 지정합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<PreventionResponse>>> getPreventionList(
		@RequestParam Long stackId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(
			mapper.toResponses(preventionService.getPreventionList(stackId, principal.getTenantId()))
		));
	}

	@Operation(summary = "방지설비 상세 조회")
	@GetMapping("/{preventionId}")
	public ResponseEntity<ApiResponse<PreventionResponse>> getPrevention(
		@PathVariable Long preventionId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(
			mapper.toResponse(preventionService.getPrevention(preventionId, principal.getTenantId()))
		));
	}

	@Operation(summary = "방지설비 수정", description = "전달하지 않은 필드는 기존 값을 유지합니다.")
	@PutMapping("/{preventionId}")
	public ResponseEntity<ApiResponse<PreventionResponse>> updatePrevention(
		@PathVariable Long preventionId,
		@RequestBody UpdatePreventionRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		Prevention prevention = preventionService.updatePrevention(preventionId, principal.getTenantId(), mapper.toUpdateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(prevention)));
	}

	@Operation(summary = "방지설비 삭제")
	@DeleteMapping("/{preventionId}")
	public ResponseEntity<ApiResponse<Void>> deletePrevention(
		@PathVariable Long preventionId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		preventionService.deletePrevention(preventionId, principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success());
	}
}
