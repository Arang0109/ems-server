package com.ensolution.ems.schedule.presentation.controller;

import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import com.ensolution.ems.schedule.application.command.detail.ScheduleDetail;
import com.ensolution.ems.schedule.application.command.list_item.ScheduleListItem;
import com.ensolution.ems.schedule.application.service.ScheduleService;
import com.ensolution.ems.schedule.presentation.mapper.ScheduleMapper;
import com.ensolution.ems.schedule.presentation.request.ChangeScheduleStatusRequest;
import com.ensolution.ems.schedule.presentation.request.CreateScheduleRequest;
import com.ensolution.ems.schedule.presentation.request.SaveSheetsRequest;
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

	@Operation(summary = "측정계획 메타 수정", description = "전달하지 않은 필드는 기존 값을 유지합니다. 완료·취소 상태는 수정할 수 없습니다.")
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
