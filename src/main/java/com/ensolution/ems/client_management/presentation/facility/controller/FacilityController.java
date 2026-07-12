package com.ensolution.ems.client_management.presentation.facility.controller;

import com.ensolution.ems.client_management.application.service.FacilityService;
import com.ensolution.ems.client_management.domain.Facility;
import com.ensolution.ems.client_management.presentation.facility.request.CreateFacilityRequest;
import com.ensolution.ems.client_management.presentation.facility.mapper.FacilityMapper;
import com.ensolution.ems.client_management.presentation.facility.response.FacilityResponse;
import com.ensolution.ems.client_management.presentation.facility.request.UpdateFacilityRequest;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Facility", description = "배출 시설(연료·시설) API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/facilities")
@RequiredArgsConstructor
public class FacilityController {

	private final FacilityService facilityService;
	private final FacilityMapper mapper;

	@Operation(summary = "배출 시설 등록")
	@PostMapping
	public ResponseEntity<ApiResponse<FacilityResponse>> createFacility(
		@RequestBody CreateFacilityRequest request
	) {
		Facility facility = facilityService.createFacility(mapper.toCreateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(facility)));
	}

	@Operation(summary = "배출 시설 목록 조회", description = "stackId 쿼리 파라미터로 측정시설을 지정합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<FacilityResponse>>> getFacilityList(
		@RequestParam Long stackId
	) {
		return ResponseEntity.ok().body(ApiResponse.success(
			mapper.toResponses(facilityService.getFacilityList(stackId))
		));
	}

	@Operation(summary = "배출 시설 상세 조회")
	@GetMapping("/{facilityId}")
	public ResponseEntity<ApiResponse<FacilityResponse>> getFacility(@PathVariable Long facilityId) {
		return ResponseEntity.ok().body(ApiResponse.success(
			mapper.toResponse(facilityService.getFacility(facilityId))
		));
	}

	@Operation(summary = "배출 시설 수정", description = "전달하지 않은 필드는 기존 값을 유지합니다.")
	@PutMapping("/{facilityId}")
	public ResponseEntity<ApiResponse<FacilityResponse>> updateFacility(
		@PathVariable Long facilityId,
		@RequestBody UpdateFacilityRequest request
	) {
		Facility facility = facilityService.updateFacility(facilityId, mapper.toUpdateCommand(request));
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(facility)));
	}

	@Operation(summary = "배출 시설 삭제")
	@DeleteMapping("/{facilityId}")
	public ResponseEntity<ApiResponse<Void>> deleteFacility(@PathVariable Long facilityId) {
		facilityService.deleteFacility(facilityId);
		return ResponseEntity.ok().body(ApiResponse.success());
	}
}
