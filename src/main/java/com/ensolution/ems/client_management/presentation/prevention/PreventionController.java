package com.ensolution.ems.client_management.presentation.prevention;

import com.ensolution.ems.client_management.application.service.PreventionCommandService;
import com.ensolution.ems.client_management.application.service.PreventionQueryService;
import com.ensolution.ems.client_management.domain.Prevention;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Prevention", description = "방지설비 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/preventions")
@RequiredArgsConstructor
public class PreventionController {

	private final PreventionCommandService preventionCommandService;
	private final PreventionQueryService preventionQueryService;
	private final PreventionMapper mapper;

	@Operation(summary = "방지설비 등록")
	@PostMapping
	public ResponseEntity<ApiResponse<PreventionResponse>> createPrevention(
		@Valid @RequestBody CreatePreventionRequest request
	) {
		Prevention prevention = preventionCommandService.createPrevention(mapper.toCreateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(prevention)));
	}

	@Operation(summary = "방지설비 목록 조회", description = "stackId 쿼리 파라미터로 측정시설을 지정합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<PreventionResponse>>> getPreventionList(
		@RequestParam Long stackId
	) {
		return ResponseEntity.ok().body(ApiResponse.success(
			mapper.toResponses(preventionQueryService.getPreventionList(stackId))
		));
	}

	@Operation(summary = "방지설비 상세 조회")
	@GetMapping("/{preventionId}")
	public ResponseEntity<ApiResponse<PreventionResponse>> getPrevention(@PathVariable Long preventionId) {
		return ResponseEntity.ok().body(ApiResponse.success(
			mapper.toResponse(preventionQueryService.getPrevention(preventionId))
		));
	}

	@Operation(summary = "방지설비 수정", description = "전달하지 않은 필드는 기존 값을 유지합니다.")
	@PutMapping("/{preventionId}")
	public ResponseEntity<ApiResponse<PreventionResponse>> updatePrevention(
		@PathVariable Long preventionId,
		@RequestBody UpdatePreventionRequest request
	) {
		Prevention prevention = preventionCommandService.updatePrevention(preventionId, mapper.toUpdateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(prevention)));
	}

	@Operation(summary = "방지설비 삭제")
	@DeleteMapping("/{preventionId}")
	public ResponseEntity<ApiResponse<Void>> deletePrevention(@PathVariable Long preventionId) {
		preventionCommandService.deletePrevention(preventionId);
		return ResponseEntity.ok().body(ApiResponse.success());
	}
}
