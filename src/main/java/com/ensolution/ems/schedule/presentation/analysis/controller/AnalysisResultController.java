package com.ensolution.ems.schedule.presentation.analysis.controller;

import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import com.ensolution.ems.schedule.application.service.AnalysisResultService;
import com.ensolution.ems.schedule.presentation.analysis.mapper.AnalysisResultMapper;
import com.ensolution.ems.schedule.presentation.analysis.request.SaveAnalysisResultsRequest;
import com.ensolution.ems.schedule.presentation.analysis.request.SaveSamplingTimesRequest;
import com.ensolution.ems.schedule.presentation.analysis.response.AnalysisResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 측정계획 실험분석정보 API.
 *
 * <p>저장 경로가 <b>탭 단위로 둘</b>이다. 실험·분석 탭과 성적서 탭이 한 항목의 필드를 나눠 소유하고
 * 서로 다른 null 시맨틱을 갖기 때문이며, 합치면 두 탭이 서로의 입력을 덮어쓴다.
 *
 * <p>단건 등록·수정·삭제 경로는 두지 않는다. 두 탭 모두 항목 표 <b>전체</b>를 보내는 일괄 저장이
 * 실제 사용 방식이고, "빈 칸 = 지움" 규약이 있어 행 삭제가 빈 값 저장과 같은 뜻이 된다.
 * 분석 결과가 측정계획 문서 안에 있어 문서 대리키도 없다 — 식별 축은 측정물질({@code pollutantId})이다.
 */
@Tag(name = "ScheduleAnalysis", description = "측정계획 실험분석정보 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/schedules/{scheduleId}/analyses")
@RequiredArgsConstructor
public class AnalysisResultController {

	private final AnalysisResultService analysisResultService;
	private final AnalysisResultMapper mapper;

	@Operation(summary = "실험분석정보 목록 조회",
		description = "측정계획의 측정항목을 성적서 표기 순서대로 반환하며, 각 항목의 판정 근거와 분석 결과를 함께 담습니다. "
			+ "아직 분석 전인 항목은 결과 칸이 모두 비어 나옵니다. "
			+ "같은 내용을 GET /api/schedules/{scheduleId} 의 snapshot.items 로도 받을 수 있습니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<AnalysisResultResponse>>> getAnalyses(
		@PathVariable Long scheduleId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponses(
			analysisResultService.getAnalyses(scheduleId, principal.getTenantId()))));
	}

	@Operation(summary = "항목별 실험분석 결과 일괄 저장",
		description = "실험·분석 탭에서 작성한 측정항목별 분석 결과를 한 번에 저장합니다. "
			+ "측정물질(pollutantId)을 키로 upsert 하므로 결과가 없던 항목은 새로 채우고, 있으면 값만 갱신합니다. "
			+ "채취시간은 이 경로로 바뀌지 않습니다 — 그 값은 성적서 탭이 소유하므로 두 탭을 동시에 열어도 "
			+ "서로를 덮어쓰지 않습니다. "
			+ "전달한 항목의 빈 값은 기존 값을 지우며(표 전체를 보내므로 빈 칸은 '지웠다'는 뜻입니다), "
			+ "요청에 없는 항목은 그대로 둡니다. "
			+ "이번 계획의 측정항목이 아닌 물질은 400, 같은 항목이 두 번 담기면 400으로 거부하며, "
			+ "완료·취소 상태의 계획은 저장할 수 없습니다.")
	@PutMapping("/results")
	public ResponseEntity<ApiResponse<List<AnalysisResultResponse>>> saveAnalysisResults(
		@PathVariable Long scheduleId,
		@Valid @RequestBody SaveAnalysisResultsRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponses(
			analysisResultService.saveAnalysisResults(
				scheduleId, principal.getTenantId(), mapper.toSaveAnalysisResultsCommand(request)))));
	}

	@Operation(summary = "성적서 항목별 채취시간 일괄 저장",
		description = "성적서 탭에서 작성한 측정항목별 채취 시작·종료 시각을 한 번에 저장합니다. "
			+ "실험실 입력값(측정분석값·측정단위·측정분석방법·분석장비)은 이 경로로 바뀌지 않습니다 — "
			+ "그 값들은 실험·분석 탭이 소유하므로 두 탭을 동시에 열어도 서로를 덮어쓰지 않습니다. "
			+ "전달한 항목의 빈 시각은 기존 값을 지우며, 요청에 없는 항목은 그대로 둡니다. "
			+ "채취시간은 현장 채취 기록지에서 자동으로 옮겨오지 않습니다 — 기록지는 알데히드류를 VOCs로 "
			+ "통칭해 시료 한 건으로 적지만 성적서는 항목마다 따로 쓰므로, 자동 복사는 틀린 시각을 남깁니다. "
			+ "이번 계획의 측정항목이 아닌 물질은 400, 같은 항목이 두 번 담기면 400으로 거부하며, "
			+ "완료·취소 상태의 계획은 저장할 수 없습니다.")
	@PutMapping("/sampling-times")
	public ResponseEntity<ApiResponse<List<AnalysisResultResponse>>> saveSamplingTimes(
		@PathVariable Long scheduleId,
		@Valid @RequestBody SaveSamplingTimesRequest request,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		return ResponseEntity.ok().body(ApiResponse.success(mapper.toResponses(
			analysisResultService.saveSamplingTimes(
				scheduleId, principal.getTenantId(), mapper.toSaveSamplingTimesCommand(request)))));
	}
}
