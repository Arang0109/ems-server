package com.ensolution.ems.client_management.presentation.pollutant.controller;

import com.ensolution.ems.client_management.application.service.PollutantService;
import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.client_management.presentation.pollutant.request.CreatePollutantRequest;
import com.ensolution.ems.client_management.presentation.pollutant.mapper.PollutantMapper;
import com.ensolution.ems.client_management.presentation.pollutant.response.PollutantResponse;
import com.ensolution.ems.client_management.presentation.pollutant.request.UpdatePollutantRequest;
import com.ensolution.ems.global.common.enums.MeasurementField;
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

@Tag(name = "Pollutant", description = "측정물질 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/pollutants")
@RequiredArgsConstructor
public class PollutantController {

	private final PollutantService pollutantService;
	private final PollutantMapper mapper;

	@Operation(summary = "측정물질 등록")
	@PostMapping
	public ResponseEntity<ApiResponse<PollutantResponse>> createPollutant(
		@Valid @RequestBody CreatePollutantRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		Pollutant pollutant = pollutantService.createPollutant(mapper.toCreateCommand(request, principal.getTenantId()));
		return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(pollutant)));
	}

	@Operation(summary = "측정물질 목록 조회")
	@GetMapping
	public ResponseEntity<ApiResponse<List<PollutantResponse>>> getPollutantList(
		@RequestParam(required = false) MeasurementField field,
		@AuthenticationPrincipal CustomUserDetails principal
		) {
		return ResponseEntity.ok(ApiResponse.success(mapper.toResponses(pollutantService.getPollutantList(field, principal.getTenantId()))));
	}

	@Operation(summary = "측정물질 상세 조회")
	@GetMapping("/{pollutantId}")
	public ResponseEntity<ApiResponse<PollutantResponse>> getPollutant(
		@PathVariable Long pollutantId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(pollutantService.getPollutant(pollutantId, principal.getTenantId()))));
	}

	@Operation(summary = "측정물질 수정", description = "전달하지 않은 필드는 기존 값을 유지합니다.")
	@PutMapping("/{pollutantId}")
	public ResponseEntity<ApiResponse<PollutantResponse>> updatePollutant(
		@PathVariable Long pollutantId,
		@RequestBody UpdatePollutantRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		Pollutant pollutant = pollutantService.updatePollutant(pollutantId, principal.getTenantId(), mapper.toUpdateCommand(request));
		return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(pollutant)));
	}

	@Operation(summary = "측정물질 삭제")
	@DeleteMapping("/{pollutantId}")
	public ResponseEntity<ApiResponse<Void>> deletePollutant(
		@PathVariable Long pollutantId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		pollutantService.deletePollutant(pollutantId, principal.getTenantId());
		return ResponseEntity.ok(ApiResponse.success());
	}
}
