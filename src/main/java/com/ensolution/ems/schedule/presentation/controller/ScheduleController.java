package com.ensolution.ems.schedule.presentation.controller;

import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import com.ensolution.ems.schedule.application.command.detail.PreviousSheetCandidate;
import com.ensolution.ems.schedule.application.command.detail.PreviousSheetDetail;
import com.ensolution.ems.schedule.application.command.detail.ScheduleDetail;
import com.ensolution.ems.schedule.application.command.event.EditorRef;
import com.ensolution.ems.schedule.application.command.list_item.ScheduleListItem;
import com.ensolution.ems.schedule.application.service.ScheduleService;
import com.ensolution.ems.schedule.presentation.mapper.ScheduleMapper;
import com.ensolution.ems.schedule.presentation.request.ChangeScheduleEquipmentsRequest;
import com.ensolution.ems.schedule.presentation.request.ChangeScheduleItemsRequest;
import com.ensolution.ems.schedule.presentation.request.ReorderScheduleItemsRequest;
import com.ensolution.ems.schedule.presentation.request.UpdateScheduleItemRequest;
import com.ensolution.ems.schedule.presentation.request.ChangeClientSnapshotRequest;
import com.ensolution.ems.schedule.presentation.request.CreateScheduleRequest;
import com.ensolution.ems.schedule.presentation.request.SaveSheetsRequest;
import com.ensolution.ems.schedule.presentation.request.UpdateBasicInfoRequest;
import com.ensolution.ems.schedule.presentation.request.UpdateScheduleRequest;
import com.ensolution.ems.schedule.domain.sheet.MeasurementCategory;
import com.ensolution.ems.schedule.presentation.response.PreviousSheetCandidateResponse;
import com.ensolution.ems.schedule.presentation.response.PreviousSheetResponse;
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
			mapper.toCreateCommand(request, principal.getTenantId(), principal.getUserId())
		);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "측정계획 목록 조회",
		description = "진행 중이거나 성적서 작성이 완료된 측정계획을 반환합니다. 취소된 계획은 담기지 않으며 "
			+ "GET /api/schedules/canceled 로 조회합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<ScheduleListResponse>>> getScheduleList(
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		List<ScheduleListItem> items = scheduleService.getScheduleList(principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toListResponses(items)));
	}

	@Operation(summary = "취소된 측정계획 목록 조회",
		description = "취소된 측정계획을 반환합니다. 취소 건에는 이력으로 남겨야 할 것과 애초에 잘못 만들어진 것이 "
			+ "섞여 있으므로, 후자는 DELETE /api/schedules/{scheduleId} 로 지웁니다. ")
	@GetMapping("/canceled")
	public ResponseEntity<ApiResponse<List<ScheduleListResponse>>> getCanceledScheduleList(
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		List<ScheduleListItem> items = scheduleService.getCanceledScheduleList(principal.getTenantId());
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

	@Operation(summary = "측정계획 메타 수정", description = "메타와 함께 문서 스냅샷의 기본정보를 갱신합니다. 전달하지 않은 필드는 기존 값을 유지합니다. ")
	@PutMapping("/{scheduleId}")
	public ResponseEntity<ApiResponse<ScheduleResponse>> updateSchedule(
		@PathVariable Long scheduleId,
		@Valid @RequestBody UpdateScheduleRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		ScheduleDetail detail = scheduleService.updateMeta(
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

	@Operation(summary = "측정계획 성적서 작성 완료 확정",
		description = "분석값을 입력 중인 측정계획을 성적서 작성 완료로 확정합니다. 이후 수정·삭제가 잠기며 측정 건수 통계에 집계됩니다. "
			+ "잘못 확정한 경우 관리자가 POST /api/schedules/{scheduleId}/reopen 으로 재개방할 수 있습니다.")
	@PostMapping("/{scheduleId}/completion")
	public ResponseEntity<ApiResponse<ScheduleResponse>> complete(
		@PathVariable Long scheduleId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		ScheduleDetail detail = scheduleService.complete(scheduleId, principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "측정계획 취소",
		description = "업무가 무산된 측정계획을 취소합니다. "
			+ "취소된 계획은 일반 목록에서 빠져 GET /api/schedules/canceled 로 따로 관리합니다 — "
			+ "잘못 등록한 계획을 곧바로 지우려면 DELETE 를 사용합니다. "
			+ "완료·취소된 계획은 다시 취소할 수 없습니다.")
	@PostMapping("/{scheduleId}/cancellation")
	public ResponseEntity<ApiResponse<ScheduleResponse>> cancel(
		@PathVariable Long scheduleId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		ScheduleDetail detail = scheduleService.cancel(scheduleId, principal.getTenantId());
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

	@Operation(summary = "측정계획 측정항목 변경",
		description = "이번 계획에서 측정할 항목을 전달한 측정물질 목록으로 전체 교체합니다. "
			+ "측정시설에 등록된 측정항목 중에서만 선택할 수 있으며, 이미 들어 있던 항목은 측정 시점 값(허용기준 등)을 유지합니다. "
			+ "계산 입력이 아니므로 측정 시트는 재계산하지 않습니다 — 빠진 항목의 측정값이 시트에 남아 있으면 "
			+ "PUT /api/schedules/{scheduleId}/sheets 로 정리해야 합니다. 원장은 변경하지 않으며 완료·취소 상태는 변경할 수 없습니다.")
	@PatchMapping("/{scheduleId}/items")
	public ResponseEntity<ApiResponse<ScheduleResponse>> changeItems(
		@PathVariable Long scheduleId,
		@Valid @RequestBody ChangeScheduleItemsRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		ScheduleDetail detail = scheduleService.changeItems(
			scheduleId, principal.getTenantId(), request.pollutantIds()
		);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(detail)));
	}

	/**
	 * 리터럴 경로라 {@code PATCH /{scheduleId}/items/{pollutantId}} 보다 먼저 매칭된다 — 순서를 바꿔 놓지 말 것.
	 */
	@Operation(summary = "측정계획 측정항목 순서 변경",
		description = "성적서에 표기할 측정항목의 순서를 바꿉니다. 배열 순서가 곧 성적서 표기 순서이며, "
			+ "기록부 서식은 한 장에 실리는 항목 수가 정해져 있어(예: 대기측정기록부 4개) 이 순서가 "
			+ "몇 번째 항목이 몇 번째 장에 들어갈지를 결정합니다. "
			+ "orderedPollutantIds 는 이 계획의 측정항목 전체여야 하며, 집합이 서버와 다르면 아무것도 저장하지 않고 거절합니다. "
			+ "항목을 더하거나 빼려면 PATCH /api/schedules/{scheduleId}/items 를 사용하세요. "
			+ "항목 내용과 측정 시트는 변경되지 않으며, 원장도 건드리지 않습니다. 완료·취소 상태는 변경할 수 없습니다.")
	@PutMapping("/{scheduleId}/items/order")
	public ResponseEntity<ApiResponse<ScheduleResponse>> reorderItems(
		@PathVariable Long scheduleId,
		@Valid @RequestBody ReorderScheduleItemsRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		ScheduleDetail detail = scheduleService.reorderItems(
			scheduleId, principal.getTenantId(), request.orderedPollutantIds()
		);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "측정계획 측정항목 정정",
		description = "이번 계획에 담긴 측정항목 하나의 측정 조건(측정주기·허용기준·산소보정)을 바로잡습니다. "
			+ "계획 수립 뒤 현장에서 배출허용기준이나 산소보정 적용 여부가 실제와 다름을 확인했을 때 사용합니다. "
			+ "고쳐지는 것은 이 회차 문서이며 측정시설 원장(stack_pollutant)은 변경되지 않습니다 — "
			+ "원장까지 함께 고치려면 PUT /api/stack-pollutants/{id} 를 따로 호출하세요. "
			+ "같은 항목의 실험분석정보가 있으면 판정 근거(허용기준·산소보정)를 함께 맞춥니다(실험실 입력값은 유지). "
			+ "허용기준·산소보정은 시트 계산식의 입력이 아니므로 측정 시트는 재계산하지 않습니다. "
			+ "이번 계획의 측정항목이 아닌 물질은 거부하며, 완료·취소 상태는 변경할 수 없습니다"
			+ "(성적서 작성이 완료된 회차를 고치려면 재개방 후 수정하고 다시 완료하면 이행 기록도 새 기준으로 다시 남습니다).")
	@PatchMapping("/{scheduleId}/items/{pollutantId}")
	public ResponseEntity<ApiResponse<ScheduleResponse>> updateItem(
		@PathVariable Long scheduleId,
		@PathVariable Long pollutantId,
		@Valid @RequestBody UpdateScheduleItemRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		ScheduleDetail detail = scheduleService.updateItem(
			scheduleId, principal.getTenantId(), pollutantId, mapper.toUpdateItemCommand(request)
		);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "측정 시트 저장",
		description = "측정값을 저장하며 서버가 계산 파이프라인을 실행해 계산 결과를 함께 반영합니다. "
			+ "요청에 담긴 카테고리의 시트만 교체하고 나머지는 서버 값을 유지하므로, 시트 삭제는 deletedSheets로 명시해야 합니다. "
			+ "읽어간 뒤 다른 사용자가 같은 시트를 먼저 저장했으면 409로 거부합니다. 완료·취소 상태는 저장할 수 없습니다.")
	@PutMapping("/{scheduleId}/sheets")
	public ResponseEntity<ApiResponse<ScheduleResponse>> saveSheets(
		@PathVariable Long scheduleId,
		@Valid @RequestBody SaveSheetsRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		ScheduleDetail detail = scheduleService.saveSheets(
			scheduleId, principal.getTenantId(),
			new EditorRef(principal.getUsername(), principal.getName()),
			request.sheets(), request.deletedSheets()
		);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(detail)));
	}

	@Operation(summary = "이전 회차 기록지 불러오기",
		description = "새로 추가한 기록지를 채울 이전 회차의 같은 기록지를 반환합니다. 저장하지는 않으므로 "
			+ "화면에서 값을 확인·수정한 뒤 PUT /api/schedules/{scheduleId}/sheets 로 저장해야 합니다. "
			+ "회차마다 쓰는 기록지가 다르므로 직전 회차만 보지 않고 그 기록지를 실제로 쓴 최근 완료 회차를 찾습니다. "
			+ "sourceScheduleId로 가져올 회차를 지정할 수 있으며, 주지 않으면 가장 최근 회차입니다. "
			+ "고를 수 있는 회차는 GET /api/schedules/{scheduleId}/sheets/{category}/previous/candidates 로 조회합니다. "
			+ "시료번호·채취 시각·기상 조건은 그 회차에만 유효한 값이므로 비워서 내려갑니다. "
			+ "불러올 기록이 없으면 data가 null입니다(첫 회차이거나 그 기록지를 처음 쓰는 경우, "
			+ "또는 지정한 회차가 후보 범위를 벗어난 경우).")
	@GetMapping("/{scheduleId}/sheets/{category}/previous")
	public ResponseEntity<ApiResponse<PreviousSheetResponse>> getPreviousSheet(
		@PathVariable Long scheduleId,
		@PathVariable MeasurementCategory category,
		@RequestParam(required = false) Long sourceScheduleId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		PreviousSheetDetail detail = scheduleService.getPreviousSheet(
			scheduleId, principal.getTenantId(), category, sourceScheduleId);
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toPreviousSheetResponse(detail)));
	}

	@Operation(summary = "이전 회차 기록지 후보 조회",
		description = "해당 기록지를 실제로 쓴 이전 완료 회차를 측정일 내림차순으로 반환합니다. "
			+ "가장 최근 회차가 늘 좋은 출발점은 아니므로(이상 조업 회차 등) 사용자가 골라 불러올 수 있게 합니다. "
			+ "시트 본문은 담기지 않습니다 — 고른 회차는 "
			+ "GET /api/schedules/{scheduleId}/sheets/{category}/previous?sourceScheduleId= 로 받습니다. "
			+ "불러올 기록이 없으면 빈 배열입니다.")
	@GetMapping("/{scheduleId}/sheets/{category}/previous/candidates")
	public ResponseEntity<ApiResponse<List<PreviousSheetCandidateResponse>>> getPreviousSheetCandidates(
		@PathVariable Long scheduleId,
		@PathVariable MeasurementCategory category,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		List<PreviousSheetCandidate> candidates = scheduleService.getPreviousSheetCandidates(
			scheduleId, principal.getTenantId(), category);
		return ResponseEntity.ok().body(
			ApiResponse.success(mapper.toPreviousSheetCandidateResponses(candidates)));
	}

	@Operation(summary = "측정계획 삭제",
		description = "잘못 등록된 측정계획을 지웁니다. 메타·세부 문서·실험분석정보를 함께 삭제하며 되돌릴 수 없습니다. "
			+ "실측 데이터가 없는 '측정 예정'과 '취소' 상태에서만 가능하며, 진행 중인 계획은 "
			+ "POST /api/schedules/{scheduleId}/cancellation 으로 먼저 취소해야 합니다.")
	@DeleteMapping("/{scheduleId}")
	public ResponseEntity<ApiResponse<Void>> deleteSchedule(
		@PathVariable Long scheduleId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		scheduleService.deleteSchedule(scheduleId, principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success());
	}

	@Operation(summary = "측정계획 재개방",
		description = "완료·취소된 측정계획을 되돌려 다시 작업할 수 있게 합니다. "
			+ "돌아가는 단계는 저장된 측정 데이터에서 재도출합니다 — 실측값이 있으면 측정 중, "
			+ "시료접수일까지 있으면 분석값 입력 중입니다. 상태가 성적서 작성 완료가 아니게 되므로 측정 건수 통계에서도 자동으로 빠집니다. ")
	@PostMapping("/{scheduleId}/reopen")
	public ResponseEntity<ApiResponse<ScheduleResponse>> reopen(
		@PathVariable Long scheduleId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		ScheduleDetail detail = scheduleService.reopen(scheduleId, principal.getTenantId());
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(detail)));
	}
}
