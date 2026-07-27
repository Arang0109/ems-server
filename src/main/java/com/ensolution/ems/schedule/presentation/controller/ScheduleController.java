package com.ensolution.ems.schedule.presentation.controller;

import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import com.ensolution.ems.schedule.application.command.detail.ScheduleDetail;
import com.ensolution.ems.schedule.application.command.list_item.ScheduleListItem;
import com.ensolution.ems.schedule.application.service.ScheduleService;
import com.ensolution.ems.schedule.presentation.mapper.ScheduleMapper;
import com.ensolution.ems.schedule.presentation.request.ChangeScheduleEquipmentsRequest;
import com.ensolution.ems.schedule.presentation.request.ChangeScheduleStatusRequest;
import com.ensolution.ems.schedule.presentation.request.ChangeClientSnapshotRequest;
import com.ensolution.ems.schedule.presentation.request.CreateScheduleRequest;
import com.ensolution.ems.schedule.presentation.request.SaveSheetsRequest;
import com.ensolution.ems.schedule.presentation.request.UpdateBasicInfoRequest;
import com.ensolution.ems.schedule.presentation.request.UpdateScheduleRequest;
import com.ensolution.ems.schedule.presentation.response.ScheduleListResponse;
import com.ensolution.ems.schedule.presentation.response.ScheduleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Schedule", description = "측정계획 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

	private final ScheduleService scheduleService;
	private final ScheduleMapper mapper;

	@Operation(summary = "측정계획 등록", description = "측정 대상·팀 id로 측정 시점 스냅샷을 조립해 세부 문서를 함께 생성합니다.")
	@PostMapping
	public ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(
		@Valid @RequestBody CreateScheduleRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		ScheduleDetail detail = scheduleService.createSchedule(
			mapper.toCreateCommand(request, principal.getTenantId())
		);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "측정계획 목록 조회")
	@GetMapping
	public ResponseEntity<ApiResponse<List<ScheduleListResponse>>> getScheduleList(
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		List<ScheduleListItem> items = scheduleService.getScheduleList(principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toListResponses(items)));
	}

	@Operation(summary = "측정계획 상세 조회")
	@GetMapping("/{scheduleId}")
	public ResponseEntity<ApiResponse<ScheduleResponse>> getSchedule(
		@PathVariable Long scheduleId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		ScheduleDetail detail = scheduleService.getSchedule(scheduleId, principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "측정계획 메타 수정", description = "메타와 함께 문서 스냅샷의 기본정보를 갱신합니다. 전달하지 않은 필드는 기존 값을 유지합니다. "
		+ "의뢰기관·사업장·측정시설 정보는 PATCH /api/schedules/{scheduleId}/client 로 수정합니다. 완료·취소 상태는 수정할 수 없습니다.")
	@PutMapping("/{scheduleId}")
	public ResponseEntity<ApiResponse<ScheduleResponse>> updateSchedule(
		@PathVariable Long scheduleId,
		@Valid @RequestBody UpdateScheduleRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		ScheduleDetail detail = scheduleService.updateSchedule(
			scheduleId, principal.getTenantId(), mapper.toUpdateCommand(request)
		);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "측정계획 기본 정보 수정",
		description = "담당자(배출시설관리자·시료채취입회자·시료분석검사자·기술책임자), 시료접수/분석완료/성적서발행일자, "
			+ "채취 시작·종료 시각, 측정자(멘토·멘티) 표기명을 수정합니다. 전달하지 않은 필드는 기존 값을 유지하며, "
			+ "계산 입력이 아니므로 측정 시트는 재계산하지 않습니다. 의뢰기관·팀 원장은 변경하지 않습니다. "
			+ "측정분야·채취일자·측정용도·내부 식별 코드는 PUT /api/schedules/{scheduleId} 를 사용합니다. "
			+ "완료·취소 상태는 수정할 수 없습니다.")
	@PatchMapping("/{scheduleId}/basic-info")
	public ResponseEntity<ApiResponse<ScheduleResponse>> updateBasicInfo(
		@PathVariable Long scheduleId,
		@Valid @RequestBody UpdateBasicInfoRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		ScheduleDetail detail = scheduleService.updateBasicInfo(
			scheduleId, principal.getTenantId(), mapper.toUpdateBasicInfoCommand(request)
		);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "측정계획 상태 변경")
	@PatchMapping("/{scheduleId}/status")
	public ResponseEntity<ApiResponse<ScheduleResponse>> changeStatus(
		@PathVariable Long scheduleId,
		@Valid @RequestBody ChangeScheduleStatusRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		ScheduleDetail detail = scheduleService.changeStatus(
			scheduleId, principal.getTenantId(), request.status()
		);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "측정계획 측정장비 변경",
		description = "장비 spec별 id로 측정장비를 교체합니다. 전달하지 않은 장비는 기존 값을 유지하며, "
			+ "팀 스냅샷의 장비 id도 함께 갱신하고 기존 측정 시트를 새 장비 spec으로 재계산합니다. "
			+ "장비 원장은 변경하지 않습니다. 완료·취소 상태는 변경할 수 없습니다.")
	@PatchMapping("/{scheduleId}/equipments")
	public ResponseEntity<ApiResponse<ScheduleResponse>> changeEquipments(
		@PathVariable Long scheduleId,
		@Valid @RequestBody ChangeScheduleEquipmentsRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		ScheduleDetail detail = scheduleService.changeEquipments(
			scheduleId, principal.getTenantId(), mapper.toChangeEquipmentsCommand(request)
		);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "측정계획 의뢰기관 스냅샷 수정",
		description = "측정계획 문서의 의뢰기관·사업장·측정시설 정보를 수정합니다. 전달하지 않은 필드는 기존 값을 유지하며, "
			+ "하위 사업장(workplace)·측정시설(workplace.stack)도 중첩 전달로 함께 부분 수정됩니다. "
			+ "배출·방지시설 목록은 전달하면 전체 교체합니다. 표준산소농도·굴뚝 형상 등 계산 입력이 바뀌면 "
			+ "기존 측정 시트를 재계산합니다. 원장은 변경하지 않습니다. 완료·취소 상태는 변경할 수 없습니다.")
	@PatchMapping("/{scheduleId}/client")
	public ResponseEntity<ApiResponse<ScheduleResponse>> changeClient(
		@PathVariable Long scheduleId,
		@Valid @RequestBody ChangeClientSnapshotRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		ScheduleDetail detail = scheduleService.changeClient(
			scheduleId, principal.getTenantId(), mapper.toChangeClientCommand(request)
		);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "측정 시트 저장", description = "측정값을 저장하며 서버가 계산 파이프라인을 실행해 계산 결과를 함께 반영합니다. 완료·취소 상태는 저장할 수 없습니다.")
	@PutMapping("/{scheduleId}/sheets")
	public ResponseEntity<ApiResponse<ScheduleResponse>> saveSheets(
		@PathVariable Long scheduleId,
		@Valid @RequestBody SaveSheetsRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		ScheduleDetail detail = scheduleService.saveSheets(
			scheduleId, principal.getTenantId(), request.sheets()
		);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "측정계획 삭제", description = "메타와 세부 문서를 함께 삭제합니다.")
	@DeleteMapping("/{scheduleId}")
	public ResponseEntity<ApiResponse<Void>> deleteSchedule(
		@PathVariable Long scheduleId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		scheduleService.deleteSchedule(scheduleId, principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success());
	}
}
