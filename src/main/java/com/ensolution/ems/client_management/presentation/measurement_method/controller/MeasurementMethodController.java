package com.ensolution.ems.client_management.presentation.measurement_method.controller;

import com.ensolution.ems.client_management.application.service.MeasurementMethodService;
import com.ensolution.ems.client_management.presentation.measurement_method.mapper.MeasurementMethodMapper;
import com.ensolution.ems.client_management.presentation.measurement_method.request.CreateMeasurementMethodRequest;
import com.ensolution.ems.client_management.presentation.measurement_method.request.UpdateMeasurementMethodRequest;
import com.ensolution.ems.client_management.presentation.measurement_method.response.MeasurementMethodResponse;
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

@Tag(name = "MeasurementMethod", description = "측정방법 API — 채취 단위·통칭 시료명·표준 채취시간을 고객사가 관리합니다")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/measurement-methods")
@RequiredArgsConstructor
public class MeasurementMethodController {

	private final MeasurementMethodService measurementMethodService;
	private final MeasurementMethodMapper mapper;

	@Operation(
		summary = "측정방법 등록",
		description = """
			측정물질을 채택할 때 고를 측정방법을 만듭니다. `sampleGrouping`이 채취 단위입니다 —
			`MERGED`(한 번의 채취로 항목 전부를 함께, 기록지에는 `mergedSampleName` 한 행)·
			`PER_ITEM`(항목별로 한 병)·`NONE`(가스상 시료 표에 행 없음).
			`samplingMinutes`는 표준 채취시간(분)으로, 이 방법을 쓰는 모든 측정항목에 한 번에 적용됩니다.
			""")
	@PostMapping
	public ResponseEntity<ApiResponse<MeasurementMethodResponse>> createMeasurementMethod(
		@Valid @RequestBody CreateMeasurementMethodRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		MeasurementMethodResponse response = mapper.toResponse(
			measurementMethodService.createMeasurementMethod(mapper.toCreateCommand(request, principal.getTenantId()))
		);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(
		summary = "기본 측정방법 채우기",
		description = """
			기본 8종(먼지·중금속·수은·현장측정·흡수액·흡착관·테드라백·카트리지)을 **이름 기준으로 멱등하게** 채웁니다.
			같은 이름이 이미 있으면 손대지 않으므로 여러 번 호출해도 안전하며, 고친 값도 되돌리지 않습니다.
			새 고객사가 측정물질을 채택하기 전에 한 번 호출합니다. 채운 뒤의 전체 목록을 돌려줍니다.
			""")
	@PostMapping("/defaults")
	public ResponseEntity<ApiResponse<List<MeasurementMethodResponse>>> ensureDefaults(
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok(ApiResponse.success(
			mapper.toResponses(measurementMethodService.ensureDefaults(principal.getTenantId()))
		));
	}

	@Operation(summary = "측정방법 목록 조회", description = "표시 순서(`sortOrder`)대로 돌려줍니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<MeasurementMethodResponse>>> getMeasurementMethodList(
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok(ApiResponse.success(
			mapper.toResponses(measurementMethodService.getMeasurementMethodList(principal.getTenantId()))
		));
	}

	@Operation(summary = "측정방법 상세 조회")
	@GetMapping("/{methodId}")
	public ResponseEntity<ApiResponse<MeasurementMethodResponse>> getMeasurementMethod(
		@PathVariable Long methodId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok(ApiResponse.success(
			mapper.toResponse(measurementMethodService.getMeasurementMethod(methodId, principal.getTenantId()))
		));
	}

	@Operation(
		summary = "측정방법 수정",
		description = """
			`name`·`sampleGrouping`은 비우면 기존 값이 유지됩니다.
			`mergedSampleName`·`samplingMinutes`는 **보낸 값이 그대로 저장**됩니다 — 비우면 지워집니다.
			채취시간을 바꾸면 이 방법을 쓰는 측정항목 전부에 즉시 반영됩니다(항목별 동기화 없음).
			이미 만들어진 측정계획의 스냅샷은 사본이라 바뀌지 않습니다.
			""")
	@PutMapping("/{methodId}")
	public ResponseEntity<ApiResponse<MeasurementMethodResponse>> updateMeasurementMethod(
		@PathVariable Long methodId,
		@Valid @RequestBody UpdateMeasurementMethodRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		MeasurementMethodResponse response = mapper.toResponse(
			measurementMethodService.updateMeasurementMethod(methodId, principal.getTenantId(), mapper.toUpdateCommand(request))
		);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "측정방법 삭제", description = "측정물질이 쓰고 있으면 삭제할 수 없습니다(409).")
	@DeleteMapping("/{methodId}")
	public ResponseEntity<ApiResponse<Void>> deleteMeasurementMethod(
		@PathVariable Long methodId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		measurementMethodService.deleteMeasurementMethod(methodId, principal.getTenantId());
		return ResponseEntity.ok(ApiResponse.success());
	}
}
