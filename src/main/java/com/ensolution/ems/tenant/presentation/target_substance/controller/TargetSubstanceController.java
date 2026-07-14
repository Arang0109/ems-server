package com.ensolution.ems.tenant.presentation.target_substance.controller;

import com.ensolution.ems.tenant.application.service.TargetSubstanceService;
import com.ensolution.ems.tenant.domain.TargetSubstance;
import com.ensolution.ems.tenant.presentation.target_substance.request.CreateTargetSubstanceRequest;
import com.ensolution.ems.tenant.presentation.target_substance.mapper.TargetSubstanceMapper;
import com.ensolution.ems.tenant.presentation.target_substance.response.TargetSubstanceResponse;
import com.ensolution.ems.tenant.presentation.target_substance.request.UpdateTargetSubstanceRequest;
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
		@Valid @RequestBody CreateTargetSubstanceRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		TargetSubstance targetSubstance = targetSubstanceService.createTargetSubstance(mapper.toCreateCommand(request, principal.getTenantId()));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(targetSubstance)));
	}

	@Operation(summary = "목표물질 목록 조회", description = "preventionId 쿼리 파라미터로 방지설비를 지정합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<TargetSubstanceResponse>>> getTargetSubstanceList(
		@RequestParam Long preventionId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(
			mapper.toResponses(targetSubstanceService.getTargetSubstanceList(preventionId, principal.getTenantId()))
		));
	}

	@Operation(summary = "목표물질 상세 조회")
	@GetMapping("/{targetSubstanceId}")
	public ResponseEntity<ApiResponse<TargetSubstanceResponse>> getTargetSubstance(
		@PathVariable Long targetSubstanceId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(
			mapper.toResponse(targetSubstanceService.getTargetSubstance(targetSubstanceId, principal.getTenantId()))
		));
	}

	@Operation(summary = "목표물질 수정", description = "전달하지 않은 필드는 기존 값을 유지합니다.")
	@PutMapping("/{targetSubstanceId}")
	public ResponseEntity<ApiResponse<TargetSubstanceResponse>> updateTargetSubstance(
		@PathVariable Long targetSubstanceId,
		@RequestBody UpdateTargetSubstanceRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		TargetSubstance targetSubstance = targetSubstanceService.updateTargetSubstance(targetSubstanceId, principal.getTenantId(), mapper.toUpdateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(targetSubstance)));
	}

	@Operation(summary = "목표물질 삭제")
	@DeleteMapping("/{targetSubstanceId}")
	public ResponseEntity<ApiResponse<Void>> deleteTargetSubstance(
		@PathVariable Long targetSubstanceId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		targetSubstanceService.deleteTargetSubstance(targetSubstanceId, principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success());
	}
}
