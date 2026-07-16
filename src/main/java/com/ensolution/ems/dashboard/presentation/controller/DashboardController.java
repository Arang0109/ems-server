package com.ensolution.ems.dashboard.presentation.controller;

import com.ensolution.ems.dashboard.application.service.DashboardService;
import com.ensolution.ems.dashboard.presentation.mapper.DashboardMapper;
import com.ensolution.ems.dashboard.presentation.response.DashboardOverviewResponse;
import com.ensolution.ems.dashboard.presentation.response.MeasurementCountChartResponse;
import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Dashboard", description = "대시보드 통계 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

	private final DashboardService dashboardService;
	private final DashboardMapper mapper;

	@Operation(summary = "대시보드 요약 통계", description = "사업장·측정시설 수와 완료 측정 총·이번달 건수를 반환합니다.")
	@GetMapping("/summary")
	public ResponseEntity<ApiResponse<DashboardOverviewResponse>> getSummary(
		@AuthenticationPrincipal CustomUserDetails principal) {
		return ResponseEntity.ok().body(ApiResponse.success(
			mapper.toResponse(dashboardService.getOverview(principal.getTenantId()))));
	}

	@Operation(summary = "월별 측정건수 추이", description = "올해 1~12월 완료 측정 건수를 월별로 반환합니다(데이터 없는 달은 0).")
	@GetMapping("/measurement-stats")
	public ResponseEntity<ApiResponse<List<MeasurementCountChartResponse>>> getMeasurementStats(
		@AuthenticationPrincipal CustomUserDetails principal) {
		return ResponseEntity.ok().body(ApiResponse.success(
			mapper.toListResponses(dashboardService.getMeasurementStats(principal.getTenantId()))));
	}
}
