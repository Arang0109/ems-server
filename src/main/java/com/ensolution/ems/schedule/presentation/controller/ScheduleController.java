package com.ensolution.ems.schedule.presentation.controller;

import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import com.ensolution.ems.schedule.application.command.detail.ScheduleDetail;
import com.ensolution.ems.schedule.application.command.list_item.ScheduleListItem;
import com.ensolution.ems.schedule.application.service.ScheduleService;
import com.ensolution.ems.schedule.presentation.mapper.ScheduleMapper;
import com.ensolution.ems.schedule.domain.ScheduleStatusLog;
import com.ensolution.ems.schedule.presentation.request.CancelScheduleRequest;
import com.ensolution.ems.schedule.presentation.request.ChangeScheduleEquipmentsRequest;
import com.ensolution.ems.schedule.presentation.request.ChangeClientSnapshotRequest;
import com.ensolution.ems.schedule.presentation.request.CreateScheduleRequest;
import com.ensolution.ems.schedule.presentation.request.ReopenScheduleRequest;
import com.ensolution.ems.schedule.presentation.request.SaveSheetsRequest;
import com.ensolution.ems.schedule.presentation.request.UpdateBasicInfoRequest;
import com.ensolution.ems.schedule.presentation.request.UpdateScheduleRequest;
import com.ensolution.ems.schedule.presentation.response.ScheduleListResponse;
import com.ensolution.ems.schedule.presentation.response.ScheduleResponse;
import com.ensolution.ems.schedule.presentation.response.ScheduleStatusLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
			mapper.toCreateCommand(request, principal.getTenantId(), principal.getUserId())
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

	@Operation(summary = "측정계획 완료 확정",
		description = "분석 중인 측정계획을 완료로 확정합니다. 이후 수정·삭제가 잠기며 측정 건수 통계에 집계됩니다. "
			+ "잘못 확정한 경우 관리자가 POST /api/schedules/{scheduleId}/reopen 으로 재개방할 수 있습니다.")
	@PostMapping("/{scheduleId}/completion")
	public ResponseEntity<ApiResponse<ScheduleResponse>> complete(
		@PathVariable Long scheduleId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		ScheduleDetail detail = scheduleService.complete(
			scheduleId, principal.getTenantId(), principal.getUserId()
		);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "측정계획 취소",
		description = "업무가 무산된 측정계획을 취소합니다. 취소 사유는 필수이며 상태 변경 이력에 기록됩니다. "
			+ "취소된 계획은 목록에 남습니다 — 잘못 등록한 계획을 목록에서 감추려면 DELETE 를 사용합니다. "
			+ "완료·취소된 계획은 다시 취소할 수 없습니다.")
	@PostMapping("/{scheduleId}/cancellation")
	public ResponseEntity<ApiResponse<ScheduleResponse>> cancel(
		@PathVariable Long scheduleId,
		@Valid @RequestBody CancelScheduleRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		ScheduleDetail detail = scheduleService.cancel(
			scheduleId, principal.getTenantId(), principal.getUserId(), request.reason()
		);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "측정계획 상태 변경 이력 조회",
		description = "등록·자동 전이·완료·취소·재개방 이력을 시간순으로 반환합니다.")
	@GetMapping("/{scheduleId}/status-logs")
	public ResponseEntity<ApiResponse<List<ScheduleStatusLogResponse>>> getStatusLogs(
		@PathVariable Long scheduleId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		List<ScheduleStatusLog> logs = scheduleService.getStatusLogs(scheduleId, principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toStatusLogResponses(logs)));
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

	@Operation(summary = "측정계획 삭제",
		description = "잘못 등록된 측정계획을 목록에서 감춥니다(soft delete). 실측 데이터가 없는 '측정 예정' 상태에서만 "
			+ "가능하며, 측정에 착수한 계획은 POST /api/schedules/{scheduleId}/cancellation 으로 취소해야 합니다. "
			+ "세부 문서는 복구를 위해 보존되며, 관리자가 POST /api/schedules/{scheduleId}/restore 로 되살릴 수 있습니다.")
	@DeleteMapping("/{scheduleId}")
	public ResponseEntity<ApiResponse<Void>> deleteSchedule(
		@PathVariable Long scheduleId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		scheduleService.deleteSchedule(scheduleId, principal.getTenantId(), principal.getUserId());
		return ResponseEntity.ok().body(ApiResponse.success());
	}

	@Operation(summary = "측정계획 재개방",
		description = "완료·취소된 측정계획을 되돌려 다시 작업할 수 있게 합니다. 재개방 사유는 필수이며 이력에 기록됩니다. "
			+ "돌아가는 단계는 저장된 측정 데이터에서 재도출합니다 — 실측값이 있으면 측정 중, "
			+ "시료접수일까지 있으면 분석 중입니다. 상태가 완료가 아니게 되므로 측정 건수 통계에서도 자동으로 빠집니다. "
			+ "완료 건의 재개방은 관리자만 할 수 있고(403), 취소 건은 담당자도 되돌릴 수 있습니다.")
	@PostMapping("/{scheduleId}/reopen")
	public ResponseEntity<ApiResponse<ScheduleResponse>> reopen(
		@PathVariable Long scheduleId,
		@Valid @RequestBody ReopenScheduleRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		// 권한이 리소스 상태에 따라 갈리므로 @PreAuthorize 가 아니라 도메인 규칙에서 판정한다.
		ScheduleDetail detail = scheduleService.reopen(
			scheduleId, principal.getTenantId(), principal.getUserId(),
			request.reason(), principal.hasAdminPrivilege()
		);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "삭제된 측정계획 목록 조회 (관리자)")
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/deleted")
	public ResponseEntity<ApiResponse<List<ScheduleListResponse>>> getDeletedScheduleList(
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		List<ScheduleListItem> items = scheduleService.getDeletedScheduleList(principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toListResponses(items)));
	}

	@Operation(summary = "삭제된 측정계획 복구 (관리자)",
		description = "감춰진 측정계획을 되살립니다. 삭제 시점의 상태를 그대로 회복합니다.")
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/{scheduleId}/restore")
	public ResponseEntity<ApiResponse<ScheduleResponse>> restore(
		@PathVariable Long scheduleId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		ScheduleDetail detail = scheduleService.restore(scheduleId, principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(detail)));
	}
}
