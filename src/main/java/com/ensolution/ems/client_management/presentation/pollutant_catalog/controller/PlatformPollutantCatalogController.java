package com.ensolution.ems.client_management.presentation.pollutant_catalog.controller;

import com.ensolution.ems.client_management.application.service.PollutantCatalogService;
import com.ensolution.ems.client_management.domain.PollutantCatalog;
import com.ensolution.ems.client_management.presentation.pollutant_catalog.mapper.PollutantCatalogMapper;
import com.ensolution.ems.client_management.presentation.pollutant_catalog.request.CreatePollutantCatalogRequest;
import com.ensolution.ems.client_management.presentation.pollutant_catalog.request.UpdatePollutantCatalogRequest;
import com.ensolution.ems.client_management.presentation.pollutant_catalog.response.PollutantCatalogResponse;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 법령 기반 측정물질 카탈로그를 관리하는 플랫폼 운영자 전용 API.
 *
 * <p>경로는 운영자 전용({@code /api/platform/**}는 {@code SecurityConfig}가 {@code PLATFORM_ADMIN}으로 보호)이지만,
 * <b>소유 모듈은 client_management</b>다. 카탈로그는 측정물질과 같은 컨텍스트이고,
 * {@code client_management → platform} 방향 참조가 금지되어 있어 platform 모듈에 둘 수 없다.
 *
 * <p>고객사 범위를 다루지 않으므로 {@code principal.getTenantId()}를 사용하지 않는다.
 */
@Tag(name = "Platform Pollutant Catalog", description = "플랫폼 운영자 전용 측정물질 카탈로그 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/platform/pollutant-catalog")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformPollutantCatalogController {

	private final PollutantCatalogService pollutantCatalogService;
	private final PollutantCatalogMapper mapper;

	@Operation(
		summary = "측정물질 카탈로그 등록",
		description = """
			모든 고객사가 공통으로 사용하는 법정 측정물질을 추가합니다. code는 등록 후 변경할 수 없습니다.
			code는 측정분야 안에서만 유일하므로, 대기 납과 수질 납처럼 분야가 다르면 같은 code를 쓸 수 있습니다.
			""")
	@PostMapping
	public ResponseEntity<ApiResponse<PollutantCatalogResponse>> createCatalog(
		@Valid @RequestBody CreatePollutantCatalogRequest request
	) {
		PollutantCatalog catalog = pollutantCatalogService.createCatalog(mapper.toCreateCommand(request));
		return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(catalog)));
	}

	@Operation(summary = "측정물질 카탈로그 목록 조회", description = "includeInactive=true면 폐지된 항목도 포함합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<PollutantCatalogResponse>>> getCatalogList(
		@RequestParam(required = false) MeasurementField field,
		@RequestParam(defaultValue = "false") boolean includeInactive
	) {
		return ResponseEntity.ok(ApiResponse.success(
			mapper.toResponses(pollutantCatalogService.getCatalogList(field, includeInactive))));
	}

	@Operation(summary = "측정물질 카탈로그 상세 조회")
	@GetMapping("/{catalogId}")
	public ResponseEntity<ApiResponse<PollutantCatalogResponse>> getCatalog(@PathVariable Long catalogId) {
		return ResponseEntity.ok(ApiResponse.success(
			mapper.toResponse(pollutantCatalogService.getCatalog(catalogId))));
	}

	@Operation(
		summary = "측정물질 카탈로그 수정",
		description = """
			전달하지 않은 필드는 기존 값을 유지합니다.

			반영 범위가 필드마다 다릅니다.
			- `field`/`method`/`phase`: 가이드가 단일 진실 소스이므로 **이미 채택한 고객사에도 즉시 반영**됩니다.
			- `nameKr`: 채택 시점에 복사되는 초기값이므로 **앞으로 채택할 고객사에만** 반영됩니다.
			  이미 채택한 고객사는 자신의 표기명을 직접 보유하므로 바뀌지 않습니다.
			""")
	@PutMapping("/{catalogId}")
	public ResponseEntity<ApiResponse<PollutantCatalogResponse>> updateCatalog(
		@PathVariable Long catalogId,
		@RequestBody UpdatePollutantCatalogRequest request
	) {
		PollutantCatalog catalog = pollutantCatalogService.updateCatalog(catalogId, mapper.toUpdateCommand(request));
		return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(catalog)));
	}

	@Operation(
		summary = "측정물질 카탈로그 폐지",
		description = "선택 목록에서 감춥니다. 이미 등록해 사용 중인 고객사의 데이터는 그대로 유지됩니다.")
	@PatchMapping("/{catalogId}/deactivate")
	public ResponseEntity<ApiResponse<PollutantCatalogResponse>> deactivateCatalog(@PathVariable Long catalogId) {
		return ResponseEntity.ok(ApiResponse.success(
			mapper.toResponse(pollutantCatalogService.deactivateCatalog(catalogId))));
	}

	@Operation(summary = "측정물질 카탈로그 폐지 해제")
	@PatchMapping("/{catalogId}/activate")
	public ResponseEntity<ApiResponse<PollutantCatalogResponse>> activateCatalog(@PathVariable Long catalogId) {
		return ResponseEntity.ok(ApiResponse.success(
			mapper.toResponse(pollutantCatalogService.activateCatalog(catalogId))));
	}
}
