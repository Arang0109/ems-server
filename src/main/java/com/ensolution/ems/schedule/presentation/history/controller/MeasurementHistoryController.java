package com.ensolution.ems.schedule.presentation.history.controller;

import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import com.ensolution.ems.schedule.application.service.MeasurementHistoryService;
import com.ensolution.ems.schedule.presentation.history.mapper.MeasurementHistoryMapper;
import com.ensolution.ems.schedule.presentation.history.response.FulfillmentBoardResponse;
import com.ensolution.ems.schedule.presentation.history.response.MeasurementRecordListResponse;
import com.ensolution.ems.schedule.presentation.history.response.PendingMeasurementListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "MeasurementHistory", description = "측정 이력·주기 이행 조회 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/measurement-records")
@RequiredArgsConstructor
public class MeasurementHistoryController {

	private final MeasurementHistoryService measurementHistoryService;
	private final MeasurementHistoryMapper mapper;

	@Operation(
		summary = "측정지점 이력 목록 조회",
		description = "완료된 측정계획의 항목별 기록을 측정일 내림차순으로 반환합니다. 연도를 주지 않으면 전체 기간입니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<MeasurementRecordListResponse>>> getRecords(
		@RequestParam Long stackId,
		@RequestParam(required = false) Integer year,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toListResponses(
			measurementHistoryService.getRecords(stackId, principal.getTenantId(), year))));
	}

	@Operation(
		summary = "연간 주기 이행 현황판",
		description = "측정항목별로 주기 구간을 펼쳐 이행 여부를 반환합니다. "
			+ "한 번도 측정하지 않은 항목도 행으로 나옵니다. 연도를 주지 않으면 올해입니다.")
	@GetMapping("/fulfillment")
	public ResponseEntity<ApiResponse<FulfillmentBoardResponse>> getFulfillmentBoard(
		@RequestParam(required = false) Long workplaceId,
		@RequestParam(required = false) Long stackId,
		@RequestParam(required = false) Integer year,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toBoardResponse(
			measurementHistoryService.getFulfillmentBoard(
				principal.getTenantId(), workplaceId, stackId, year))));
	}

	@Operation(
		summary = "미이행·기한임박 항목 조회",
		description = "기한이 지났거나 지정 일수 안에 기한이 닿는 주기 구간을 임박순으로 반환합니다. "
			+ "기한이 지난 구간은 남은 일수가 음수이며 목록 앞에 옵니다.")
	@GetMapping("/pending")
	public ResponseEntity<ApiResponse<List<PendingMeasurementListResponse>>> getPendingMeasurements(
		@RequestParam(required = false) Integer year,
		@RequestParam(defaultValue = "30") int withinDays,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toPendingListResponses(
			measurementHistoryService.getPendingMeasurements(principal.getTenantId(), year, withinDays))));
	}
}
