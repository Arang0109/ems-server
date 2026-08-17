package com.ensolution.ems.client_management.presentation.pollutant.controller;

import com.ensolution.ems.client_management.application.service.PollutantService;
import com.ensolution.ems.client_management.domain.Pollutant;
import com.ensolution.ems.client_management.presentation.pollutant.request.CreatePollutantRequest;
import com.ensolution.ems.client_management.presentation.pollutant.mapper.PollutantMapper;
import com.ensolution.ems.client_management.presentation.pollutant.response.PollutantListResponse;
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

	@Operation(
		summary = "측정물질 등록",
		description = """
			지원 물질 가이드에서 `catalogId`로 물질을 채택합니다. 가이드에 없는 물질은 등록할 수 없습니다.
			`nameKr`을 비워 두면 가이드의 표준 국문명이 복사되며, 이후 표기명·시험장비·시험방법은 고객사가 관리합니다.
			측정분야·측정방법·형태는 가이드가 정하므로 요청에 담지 않습니다.
			""")
	@PostMapping
	public ResponseEntity<ApiResponse<PollutantResponse>> createPollutant(
		@Valid @RequestBody CreatePollutantRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		Pollutant pollutant = pollutantService.createPollutant(mapper.toCreateCommand(request, principal.getTenantId()));
		return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(pollutant)));
	}

	@Operation(
		summary = "선택 가능한 측정물질 목록 조회",
		description = """
			지원 물질 가이드와 이 고객사가 채택한 측정물질을 합쳐 돌려줍니다.
			각 항목의 `source`는 다음을 뜻합니다.
			- `CATALOG`: 가이드에 있으나 아직 채택하지 않음 (`pollutantId`와 고객사 입력값이 null)
			- `REGISTERED`: 채택해 보유 중

			`code`는 모든 고객사에서 동일하므로 특정 물질을 판별할 때 사용합니다.
			단 code는 측정분야 안에서만 유일합니다(대기 납·수질 납이 모두 `PB`). 측정물질을 지목해
			채택할 때는 `catalogId`를 사용하세요.
			`includeCatalog=false`를 주면 이 고객사가 채택한 것만 조회합니다.
			""")
	@GetMapping
	public ResponseEntity<ApiResponse<List<PollutantListResponse>>> getPollutantList(
		@RequestParam(required = false) MeasurementField field,
		@RequestParam(defaultValue = "true") boolean includeCatalog,
		@AuthenticationPrincipal CustomUserDetails principal
		) {
		return ResponseEntity.ok(ApiResponse.success(mapper.toListResponses(
			pollutantService.getPollutantList(field, principal.getTenantId(), includeCatalog))));
	}

	@Operation(
		summary = "측정물질 상세 조회",
		description = "고객사가 관리하는 값(표기명·시험장비·시험방법)에 가이드 값(code·측정분야·측정방법·형태)을 "
			+ "채워 반환합니다. 목록 API와 같은 값입니다.")
	@GetMapping("/{pollutantId}")
	public ResponseEntity<ApiResponse<PollutantResponse>> getPollutant(
		@PathVariable Long pollutantId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(pollutantService.getPollutant(pollutantId, principal.getTenantId()))));
	}

	@Operation(
		summary = "측정물질 수정",
		description = "전달하지 않은 필드는 기존 값을 유지합니다. "
			+ "어떤 가이드 항목인지와 측정분야·측정방법·형태는 수정 대상이 아닙니다.")
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
