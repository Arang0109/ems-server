package com.ensolution.ems.client_management.presentation.stack.controller;

import com.ensolution.ems.client_management.application.command.detail.StackDetail;
import com.ensolution.ems.client_management.application.service.StackService;
import com.ensolution.ems.client_management.domain.Stack;
import com.ensolution.ems.client_management.presentation.stack.mapper.StackMapper;
import com.ensolution.ems.client_management.presentation.stack.request.CreateStackRequest;
import com.ensolution.ems.client_management.presentation.stack.request.UpdateStackRequest;
import com.ensolution.ems.client_management.presentation.stack.response.StackDetailResponse;
import com.ensolution.ems.client_management.presentation.stack.response.StackResponse;
import com.ensolution.ems.client_management.presentation.stack.response.StackListResponse;
import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Stack", description = "측정시설 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/stacks")
@RequiredArgsConstructor
public class StackController {

	private final StackService stackService;
	private final StackMapper mapper;

	@Operation(summary = "측정시설 등록")
	@PostMapping()
	public ResponseEntity<ApiResponse<StackResponse>> createStack(
		@Valid @RequestBody CreateStackRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		Stack stack = stackService.createStack(mapper.toCreateCommand(request, principal.getTenantId()));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(stack)));
	}

	@Operation(summary = "측정시설 목록 조회", description = "workplaceId 쿼리 파라미터로 사업장을 지정합니다.")
	@GetMapping()
	public ResponseEntity<ApiResponse<List<StackListResponse>>> getStackList(
		@RequestParam(required = false) Long workplaceId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toListResponses(
				stackService.getStackList(workplaceId, principal.getTenantId())
		)));
	}

	@Operation(summary = "측정시설 상세 조회")
	@GetMapping("/{stackId}")
	public ResponseEntity<ApiResponse<StackDetailResponse>> getStackDetail(
		@PathVariable Long stackId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		StackDetail detail = stackService.getStackDetail(stackId, principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toDetailResponse(detail)));
	}

	@Operation(summary = "측정시설 수정", description = "전달하지 않은 필드는 기존 값을 유지합니다.")
	@PutMapping("/{stackId}")
	public ResponseEntity<ApiResponse<StackResponse>> updateStack(
		@PathVariable Long stackId,
		@RequestBody UpdateStackRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		Stack stack = stackService.updateStack(stackId, principal.getTenantId(), mapper.toUpdateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(stack)));
	}

	@Operation(summary = "측정시설 삭제")
	@DeleteMapping("/{stackId}")
	public ResponseEntity<ApiResponse<Void>> deleteStack(
		@PathVariable Long stackId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		stackService.deleteStack(stackId, principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success());
	}
}
