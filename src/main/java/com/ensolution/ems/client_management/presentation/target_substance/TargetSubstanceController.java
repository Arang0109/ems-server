package com.ensolution.ems.client_management.presentation.target_substance;

import com.ensolution.ems.client_management.application.service.TargetSubstanceService;
import com.ensolution.ems.client_management.domain.TargetSubstance;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "TargetSubstance", description = "방지설비 목표물질 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/target-substances")
@RequiredArgsConstructor
public class TargetSubstanceController {

	private final TargetSubstanceService targetSubstanceService;
	private final TargetSubstanceMapper mapper;

	@Operation(summary = "목표물질 등록")
	@PostMapping
	public ResponseEntity<ApiResponse<TargetSubstanceResponse>> createTargetSubstance(
		@Valid @RequestBody CreateTargetSubstanceRequest request
	) {
		TargetSubstance targetSubstance = targetSubstanceService.createTargetSubstance(mapper.toCreateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(targetSubstance)));
	}

	@Operation(summary = "목표물질 목록 조회", description = "preventionId 쿼리 파라미터로 방지설비를 지정합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<TargetSubstanceResponse>>> getTargetSubstanceList(
		@RequestParam Long preventionId
	) {
		return ResponseEntity.ok().body(ApiResponse.success(
			mapper.toResponses(targetSubstanceService.getTargetSubstanceList(preventionId))
		));
	}

	@Operation(summary = "목표물질 상세 조회")
	@GetMapping("/{targetSubstanceId}")
	public ResponseEntity<ApiResponse<TargetSubstanceResponse>> getTargetSubstance(
		@PathVariable Long targetSubstanceId
	) {
		return ResponseEntity.ok().body(ApiResponse.success(
			mapper.toResponse(targetSubstanceService.getTargetSubstance(targetSubstanceId))
		));
	}

	@Operation(summary = "목표물질 수정", description = "전달하지 않은 필드는 기존 값을 유지합니다.")
	@PutMapping("/{targetSubstanceId}")
	public ResponseEntity<ApiResponse<TargetSubstanceResponse>> updateTargetSubstance(
		@PathVariable Long targetSubstanceId,
		@RequestBody UpdateTargetSubstanceRequest request
	) {
		TargetSubstance targetSubstance = targetSubstanceService.updateTargetSubstance(targetSubstanceId, mapper.toUpdateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(targetSubstance)));
	}

	@Operation(summary = "목표물질 삭제")
	@DeleteMapping("/{targetSubstanceId}")
	public ResponseEntity<ApiResponse<Void>> deleteTargetSubstance(@PathVariable Long targetSubstanceId) {
		targetSubstanceService.deleteTargetSubstance(targetSubstanceId);
		return ResponseEntity.ok().body(ApiResponse.success());
	}
}
