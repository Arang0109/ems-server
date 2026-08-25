package com.ensolution.ems.schedule.presentation.analysis.controller;

import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import com.ensolution.ems.schedule.application.service.AnalysisRecordService;
import com.ensolution.ems.schedule.presentation.analysis.mapper.AnalysisRecordMapper;
import com.ensolution.ems.schedule.presentation.analysis.request.CreateAnalysisRecordRequest;
import com.ensolution.ems.schedule.presentation.analysis.request.SaveAnalysisResultsRequest;
import com.ensolution.ems.schedule.presentation.analysis.request.SaveSamplingTimesRequest;
import com.ensolution.ems.schedule.presentation.analysis.request.UpdateAnalysisRecordRequest;
import com.ensolution.ems.schedule.presentation.analysis.response.AnalysisRecordResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "ScheduleAnalysis", description = "측정계획 실험분석정보 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/schedules/{scheduleId}/analyses")
@RequiredArgsConstructor
public class AnalysisRecordController {

	private final AnalysisRecordService analysisRecordService;
	private final AnalysisRecordMapper mapper;

	@Operation(summary = "실험분석정보 등록", description = "이번 측정계획의 측정항목 하나에 대한 분석 결과를 등록합니다.")
	@PostMapping
	public ResponseEntity<ApiResponse<AnalysisRecordResponse>> createAnalysis(
		@PathVariable Long scheduleId,
		@Valid @RequestBody CreateAnalysisRecordRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(
			analysisRecordService.createAnalysis(
				scheduleId, principal.getTenantId(), mapper.toCreateCommand(request)))));
	}

	@Operation(summary = "실험분석정보 목록 조회", description = "측정계획의 분석 결과를 등록순으로 반환합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<AnalysisRecordResponse>>> getAnalyses(
		@PathVariable Long scheduleId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponses(
			analysisRecordService.getAnalyses(scheduleId, principal.getTenantId()))));
	}

	@Operation(summary = "실험분석정보 단건 조회")
	@GetMapping("/{analysisId}")
	public ResponseEntity<ApiResponse<AnalysisRecordResponse>> getAnalysis(
		@PathVariable Long scheduleId,
		@PathVariable String analysisId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(
			analysisRecordService.getAnalysis(scheduleId, principal.getTenantId(), analysisId))));
	}

	/**
	 * 리터럴 경로라 {@code PUT /{analysisId}} 보다 먼저 매칭된다 — 순서를 바꿔 놓지 말 것.
	 */
	@Operation(summary = "항목별 실험분석 결과 일괄 저장",
		description = "실험·분석 탭에서 작성한 측정항목별 분석 결과를 한 번에 저장합니다. "
			+ "측정물질(pollutantId)을 키로 upsert 하므로 기록이 없는 항목은 새로 만들고, 있으면 값만 갱신합니다 — "
			+ "호출자가 문서 id로 신규·기존을 가릴 필요가 없어, 성적서 탭이 먼저 문서를 만든 뒤에도 충돌하지 않습니다. "
			+ "채취시간은 이 경로로 바뀌지 않습니다 — 그 값은 성적서 탭이 소유하므로 두 탭을 동시에 열어도 "
			+ "서로를 덮어쓰지 않습니다. "
			+ "전달한 항목의 빈 값은 기존 값을 지우며, 요청에 없는 항목은 그대로 둡니다. "
			+ "이번 계획의 측정항목이 아닌 물질은 400, 같은 항목이 두 번 담기면 400으로 거부하며, "
			+ "완료·취소 상태의 계획은 저장할 수 없습니다.")
	@PutMapping("/results")
	public ResponseEntity<ApiResponse<List<AnalysisRecordResponse>>> saveAnalysisResults(
		@PathVariable Long scheduleId,
		@Valid @RequestBody SaveAnalysisResultsRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponses(
			analysisRecordService.saveAnalysisResults(
				scheduleId, principal.getTenantId(), mapper.toSaveAnalysisResultsCommand(request)))));
	}

	/**
	 * 리터럴 경로라 {@code PUT /{analysisId}} 보다 먼저 매칭된다 — 순서를 바꿔 놓지 말 것.
	 */
	@Operation(summary = "성적서 항목별 채취시간 일괄 저장",
		description = "성적서 탭에서 작성한 측정항목별 채취 시작·종료 시각을 한 번에 저장합니다. "
			+ "기록이 없는 항목은 새로 만들고, 있으면 시각만 갱신합니다. "
			+ "실험실 입력값(측정분석값·측정단위·측정분석방법·분석장비)은 이 경로로 바뀌지 않습니다 — "
			+ "그 값들은 실험·분석 탭이 소유하므로 두 탭을 동시에 열어도 서로를 덮어쓰지 않습니다. "
			+ "전달한 항목의 빈 시각은 기존 값을 지우며, 요청에 없는 항목은 그대로 둡니다. "
			+ "채취시간은 현장 채취 기록지에서 자동으로 옮겨오지 않습니다 — 기록지는 알데히드류를 VOCs로 "
			+ "통칭해 시료 한 건으로 적지만 성적서는 항목마다 따로 쓰므로, 자동 복사는 틀린 시각을 남깁니다. "
			+ "이번 계획의 측정항목이 아닌 물질은 400, 같은 항목이 두 번 담기면 400으로 거부하며, "
			+ "완료·취소 상태의 계획은 저장할 수 없습니다.")
	@PutMapping("/sampling-times")
	public ResponseEntity<ApiResponse<List<AnalysisRecordResponse>>> saveSamplingTimes(
		@PathVariable Long scheduleId,
		@Valid @RequestBody SaveSamplingTimesRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponses(
			analysisRecordService.saveSamplingTimes(
				scheduleId, principal.getTenantId(), mapper.toSaveSamplingTimesCommand(request)))));
	}

	@Operation(summary = "실험분석정보 수정",
		description = "측정분석값·측정단위·측정분석방법·분석장비를 수정합니다. 전달하지 않은 필드는 기존 값을 유지합니다. "
			+ "허용기준치·산소농도보정 적용 여부는 측정 시점 원장 값이므로 수정 대상이 아니며, "
			+ "대상 측정항목을 바꾸려면 삭제 후 다시 등록해야 합니다. 완료·취소 상태의 계획은 수정할 수 없습니다.")
	@PutMapping("/{analysisId}")
	public ResponseEntity<ApiResponse<AnalysisRecordResponse>> updateAnalysis(
		@PathVariable Long scheduleId,
		@PathVariable String analysisId,
		@Valid @RequestBody UpdateAnalysisRecordRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponse(
			analysisRecordService.updateAnalysis(
				scheduleId, principal.getTenantId(), analysisId, mapper.toUpdateCommand(request)))));
	}

	@Operation(summary = "실험분석정보 삭제", description = "잘못 입력된 분석 결과를 지웁니다. 같은 측정항목으로 다시 등록할 수 있습니다. ")
	@DeleteMapping("/{analysisId}")
	public ResponseEntity<ApiResponse<Void>> deleteAnalysis(
		@PathVariable Long scheduleId,
		@PathVariable String analysisId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		analysisRecordService.deleteAnalysis(scheduleId, principal.getTenantId(), analysisId);
		return ResponseEntity.ok().body(ApiResponse.success());
	}
}
