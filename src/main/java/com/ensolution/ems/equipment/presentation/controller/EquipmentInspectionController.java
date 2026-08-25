package com.ensolution.ems.equipment.presentation.controller;

import com.ensolution.ems.equipment.application.service.EquipmentService;
import com.ensolution.ems.equipment.domain.InspectionRecord;
import com.ensolution.ems.equipment.presentation.mapper.InspectionRecordMapper;
import com.ensolution.ems.equipment.presentation.request.RecordInspectionRequest;
import com.ensolution.ems.equipment.presentation.response.InspectionRecordResponse;
import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Equipment Inspection", description = "측정 장비 검사 이력 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/equipments/{equipmentId}/inspections")
@RequiredArgsConstructor
public class EquipmentInspectionController {

	private final EquipmentService equipmentService;
	private final InspectionRecordMapper mapper;

	@Operation(
		summary = "검사 실시 기록",
		description = "이력을 남기고 장비의 해당 검사 항목 최종 수검일을 갱신합니다. 검사 대상이 아닌 종류는 기록할 수 없습니다."
	)
	@PostMapping
	public ResponseEntity<ApiResponse<InspectionRecordResponse>> recordInspection(
		@PathVariable String equipmentId,
		@Valid @RequestBody RecordInspectionRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		InspectionRecord record = equipmentService.recordInspection(
			mapper.toRecordCommand(request, equipmentId, principal.getTenantId())
		);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(record)));
	}

	@Operation(summary = "검사 이력 목록 조회", description = "최근 수검일 순으로 반환합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<InspectionRecordResponse>>> getInspectionRecords(
		@PathVariable String equipmentId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		List<InspectionRecord> records = equipmentService.getInspectionRecords(
			equipmentId, principal.getTenantId()
		);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponses(records)));
	}
}
