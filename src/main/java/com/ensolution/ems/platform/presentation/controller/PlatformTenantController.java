package com.ensolution.ems.platform.presentation.controller;

import com.ensolution.ems.global.web.ApiResponse;
import com.ensolution.ems.platform.application.service.PlatformService;
import com.ensolution.ems.platform.domain.Tenant;
import com.ensolution.ems.platform.presentation.mapper.TenantMapper;
import com.ensolution.ems.platform.presentation.request.ProvisionTenantRequest;
import com.ensolution.ems.platform.presentation.response.TenantListResponse;
import com.ensolution.ems.platform.presentation.response.TenantResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 플랫폼 운영자(PLATFORM_ADMIN) 전용 고객사(테넌트) 관리 API.
 * 경로 /api/platform/** 는 SecurityConfig에서 hasRole("PLATFORM_ADMIN")으로 보호된다.
 */
@Tag(name = "Platform Tenant", description = "플랫폼 운영자 전용 고객사 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/platform/tenants")
@RequiredArgsConstructor
public class PlatformTenantController {

	private final PlatformService platformService;
	private final TenantMapper mapper;

	@Operation(summary = "고객사 발급", description = "고객사(테넌트)와 초기 관리자(ADMIN) 계정을 함께 생성합니다.")
	@PostMapping()
	public ResponseEntity<ApiResponse<TenantResponse>> provisionTenant(
		@Valid @RequestBody ProvisionTenantRequest request
	) {
		Tenant tenant = platformService.provisionTenant(mapper.toProvisionCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(tenant)));
	}

	@Operation(summary = "고객사 목록 조회")
	@GetMapping()
	public ResponseEntity<ApiResponse<List<TenantListResponse>>> getTenantList() {
		return ResponseEntity.ok().body(ApiResponse.success(
			mapper.toListResponses(platformService.getTenantList())
		));
	}

	@Operation(summary = "고객사 상세 조회")
	@GetMapping("/{tenantId}")
	public ResponseEntity<ApiResponse<TenantResponse>> getTenant(
		@PathVariable Long tenantId
	) {
		Tenant tenant = platformService.getTenant(tenantId);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(tenant)));
	}
}
