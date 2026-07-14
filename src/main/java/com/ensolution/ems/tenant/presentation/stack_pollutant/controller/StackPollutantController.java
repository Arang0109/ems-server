package com.ensolution.ems.tenant.presentation.stack_pollutant.controller;

import com.ensolution.ems.tenant.application.service.StackPollutantService;
import com.ensolution.ems.tenant.domain.StackPollutant;
import com.ensolution.ems.tenant.presentation.stack_pollutant.mapper.StackPollutantMapper;
import com.ensolution.ems.tenant.presentation.stack_pollutant.request.CreateStackPollutantRequest;
import com.ensolution.ems.tenant.presentation.stack_pollutant.response.StackPollutantResponse;
import com.ensolution.ems.tenant.presentation.stack_pollutant.response.StackPollutantListResponse;
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

@Tag(name = "StackPollutant", description = "시설별 측정물질 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/stack-pollutants")
@RequiredArgsConstructor
public class StackPollutantController {

	private final StackPollutantService stackPollutantService;
	private final StackPollutantMapper mapper;

	@Operation(summary = "시설별 측정물질 등록")
	@PostMapping
	public ResponseEntity<ApiResponse<StackPollutantResponse>> registerPollutant(
		@Valid @RequestBody CreateStackPollutantRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		StackPollutant stackPollutant = stackPollutantService.createStackPollutant(mapper.toCreateCommand(request, principal.getTenantId()));
		return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(stackPollutant)));
	}

	@Operation(summary = "시설별 측정물질 목록 조회", description = "stackId 쿼리 파라미터로 시설을 지정합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<StackPollutantListResponse>>> getStackPollutantList(
		@RequestParam Long stackId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok(ApiResponse.success(
			mapper.toListResponses(stackPollutantService.getStackPollutantList(stackId, principal.getTenantId()))
		));
	}

	@Operation(summary = "시설별 측정물질 삭제")
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> removeStackPollutant(
		@PathVariable Long id,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		stackPollutantService.removeStackPollutant(id, principal.getTenantId());
		return ResponseEntity.ok(ApiResponse.success());
	}
}
