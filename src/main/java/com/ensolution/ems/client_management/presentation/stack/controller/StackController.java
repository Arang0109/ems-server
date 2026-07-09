package com.ensolution.ems.client_management.presentation.stack.controller;

import com.ensolution.ems.client_management.application.command.detail.StackDetail;
import com.ensolution.ems.client_management.application.service.StackCommandService;
import com.ensolution.ems.client_management.application.service.StackQueryService;
import com.ensolution.ems.client_management.domain.Stack;
import com.ensolution.ems.client_management.presentation.stack.mapper.StackMapper;
import com.ensolution.ems.client_management.presentation.stack.request.CreateStackRequest;
import com.ensolution.ems.client_management.presentation.stack.request.UpdateStackRequest;
import com.ensolution.ems.client_management.presentation.stack.response.StackDetailResponse;
import com.ensolution.ems.client_management.presentation.stack.response.StackResponse;
import com.ensolution.ems.client_management.presentation.stack.response.StackTableListResponse;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Stack", description = "측정시설 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/stacks")
@RequiredArgsConstructor
public class StackController {

	private final StackCommandService stackCommandService;
	private final StackQueryService stackQueryService;
	private final StackMapper mapper;

	@Operation(summary = "측정시설 등록")
	@PostMapping()
	public ResponseEntity<ApiResponse<StackResponse>> createStack(
		@Valid @RequestBody CreateStackRequest request
	) {
		Stack stack = stackCommandService.createStack(mapper.toCreateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(stack)));
	}

	@Operation(summary = "측정시설 목록 조회", description = "workplaceId 쿼리 파라미터로 사업장을 지정합니다.")
	@GetMapping()
	public ResponseEntity<ApiResponse<List<StackTableListResponse>>> getStackList(
		@RequestParam(required = false) Long workplaceId
	) {
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toTableListResponses(
				stackQueryService.getStackList(workplaceId)
		)));
	}

	@Operation(summary = "측정시설 상세 조회")
	@GetMapping("/{stackId}")
	public ResponseEntity<ApiResponse<StackDetailResponse>> getStackDetail(@PathVariable Long stackId) {
		StackDetail detail = stackQueryService.getStackDetail(stackId);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toDetailResponse(detail)));
	}

	@Operation(summary = "측정시설 수정", description = "전달하지 않은 필드는 기존 값을 유지합니다.")
	@PutMapping("/{stackId}")
	public ResponseEntity<ApiResponse<StackResponse>> updateStack(
		@PathVariable Long stackId,
		@RequestBody UpdateStackRequest request
	) {
		Stack stack = stackCommandService.updateStack(stackId, mapper.toUpdateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(stack)));
	}

	@Operation(summary = "측정시설 삭제")
	@DeleteMapping("/{stackId}")
	public ResponseEntity<ApiResponse<Void>> deleteStack(@PathVariable Long stackId) {
		stackCommandService.deleteStack(stackId);
		return ResponseEntity.ok().body(ApiResponse.success());
	}
}
