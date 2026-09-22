package com.ensolution.ems.schedule.presentation.controller;

import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.global.web.ApiResponse;
import com.ensolution.ems.schedule.application.service.ScheduleExportService;
import com.ensolution.ems.schedule.presentation.mapper.ScheduleMapper;
import com.ensolution.ems.schedule.presentation.response.TemplateCheckResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 측정계획 엑셀 내보내기 API. 고객사가 자기 양식에 jxls 문법을 적용해 업로드하면 측정 데이터를 채워 반환한다.
 * 바이너리(엑셀/ZIP) 파일 다운로드이므로 {@code ApiResponse} 래핑 대신 파일 바이트를 직접 반환한다.
 * <ul>
 *   <li>채취기록부: 측정 시트별 xlsx(원장 데이터 포함)를 하나의 ZIP으로 묶어 반환.</li>
 * </ul>
 * 템플릿 검사({@link #checkTemplate})는 바이너리가 아니라 판정 결과이므로 봉투 예외가 아니다 — {@code ApiResponse}를 쓴다.
 * 경로 {@code /sampling-records/template-check}는 전 세그먼트가 리터럴이라 {@code /{scheduleId}/...} 패턴과 충돌하지 않는다.
 */
@Tag(name = "Schedule Export", description = "측정계획 엑셀 내보내기 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleExportController {

	private final ScheduleExportService exportService;
	private final ScheduleMapper mapper;

	@Operation(summary = "채취기록부 다운로드(ZIP)",
		description = "업로드한 Jxls 템플릿(단일 시트 바인딩)에 측정 시트별 데이터를 채워, 시트당 파일 하나씩을 ZIP으로 묶어 반환합니다.")
	@PostMapping(value = "/{scheduleId}/sampling-records/export", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<byte[]> exportSamplingRecords(
		@PathVariable Long scheduleId,
		@RequestParam("template") MultipartFile template,
		@AuthenticationPrincipal CustomUserDetails principal
	) throws IOException {
		byte[] result = exportService.exportSamplingRecords(scheduleId, principal.getTenantId(), template.getBytes());

		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType("application/zip"))
			.header(HttpHeaders.CONTENT_DISPOSITION, attachment("채취기록부-" + scheduleId + ".zip"))
			.body(result);
	}

	@Operation(summary = "채취기록부 템플릿 검사",
		description = "템플릿을 렌더링하지 않고 셀 텍스트의 ${...}와 메모의 jx: 명령을 읽어, 바인딩 계약(plan·sheet·items·custom 등)과 "
			+ "이 고객사의 커스텀 필드 정의에 없는 이름을 셀 주소와 함께 돌려줍니다. "
			+ "렌더링은 없는 이름을 오류 없이 빈칸으로 넘기므로, 양식을 등록하기 전에 이 검사로 오타를 잡습니다. "
			+ "issues 가 비어 있으면 valid 입니다.")
	@PostMapping(value = "/sampling-records/template-check", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<TemplateCheckResponse>> checkTemplate(
		@RequestParam("template") MultipartFile template,
		@AuthenticationPrincipal CustomUserDetails principal
	) throws IOException {
		TemplateCheckResponse response = mapper.toTemplateCheckResponse(
			exportService.checkTemplate(principal.getTenantId(), template.getBytes()));
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	/** Content-Disposition 헤더값 생성(파일명 UTF-8 인코딩). */
	private String attachment(String filename) {
		String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
		return "attachment; filename*=UTF-8''" + encoded;
	}
}
