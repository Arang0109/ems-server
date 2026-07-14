package com.ensolution.ems.equipment.presentation.controller;

import com.ensolution.ems.equipment.application.service.EquipmentService;
import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.equipment.domain.Equipment;
import com.ensolution.ems.equipment.presentation.mapper.EquipmentMapper;
import com.ensolution.ems.equipment.presentation.request.ChangeEquipmentStatusRequest;
import com.ensolution.ems.equipment.presentation.request.CreateEquipmentRequest;
import com.ensolution.ems.equipment.presentation.request.UpdateEquipmentRequest;
import com.ensolution.ems.equipment.presentation.response.EquipmentResponse;
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

@Tag(name = "Equipment", description = "측정 장비 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/equipments")
@RequiredArgsConstructor
public class EquipmentController {

	private final EquipmentService equipmentService;
	private final EquipmentMapper mapper;

	@Operation(summary = "장비 등록")
	@PostMapping()
	public ResponseEntity<ApiResponse<EquipmentResponse>> createEquipment(
		@Valid @RequestBody CreateEquipmentRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		Equipment equipment = equipmentService.createEquipment(
			mapper.toCreateCommand(request, principal.getTenantId())
		);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(equipment)));
	}

	@Operation(summary = "장비 목록 조회", description = "type 쿼리 파라미터로 장비 유형을 필터링합니다.")
	@GetMapping()
	public ResponseEntity<ApiResponse<List<EquipmentResponse>>> getEquipmentList(
		@RequestParam(required = false) EquipType type,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		List<Equipment> equipments = equipmentService.getEquipmentList(type, principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponses(equipments)));
	}

	@Operation(summary = "장비 상세 조회")
	@GetMapping("/{equipmentId}")
	public ResponseEntity<ApiResponse<EquipmentResponse>> getEquipment(
		@PathVariable String equipmentId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		Equipment equipment = equipmentService.getEquipment(equipmentId, principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(equipment)));
	}

	@Operation(summary = "장비 수정", description = "전달하지 않은 필드는 기존 값을 유지합니다.")
	@PutMapping("/{equipmentId}")
	public ResponseEntity<ApiResponse<EquipmentResponse>> updateEquipment(
		@PathVariable String equipmentId,
		@Valid @RequestBody UpdateEquipmentRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		Equipment equipment = equipmentService.updateEquipment(
			equipmentId, principal.getTenantId(), mapper.toUpdateCommand(request)
		);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(equipment)));
	}

	@Operation(summary = "장비 상태 변경")
	@PatchMapping("/{equipmentId}/status")
	public ResponseEntity<ApiResponse<EquipmentResponse>> changeStatus(
		@PathVariable String equipmentId,
		@Valid @RequestBody ChangeEquipmentStatusRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		Equipment equipment = equipmentService.changeStatus(
			equipmentId, principal.getTenantId(), request.status()
		);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(equipment)));
	}

	@Operation(summary = "장비 삭제", description = "상태를 DELETED 로 전환하는 소프트 삭제입니다.")
	@DeleteMapping("/{equipmentId}")
	public ResponseEntity<ApiResponse<Void>> deleteEquipment(
		@PathVariable String equipmentId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		equipmentService.deleteEquipment(equipmentId, principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success());
	}
}
