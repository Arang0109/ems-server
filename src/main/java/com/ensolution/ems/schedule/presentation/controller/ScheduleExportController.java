package com.ensolution.ems.schedule.presentation.controller;

import com.ensolution.ems.global.security.user.CustomUserDetails;
import com.ensolution.ems.schedule.application.service.ScheduleExportService;
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
 *   <li>성적서: 전 시트 데이터를 담은 단일 xlsx.</li>
 *   <li>채취기록부: 측정 시트별 xlsx(원장 데이터 포함)를 하나의 ZIP으로 묶어 반환.</li>
 * </ul>
 */
@Tag(name = "Schedule Export", description = "측정계획 엑셀 내보내기 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleExportController {

	private static final MediaType XLSX = MediaType.parseMediaType(
		"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private final ScheduleExportService exportService;

	@Operation(summary = "성적서 발행(엑셀)",
		description = "업로드한 jxls 템플릿(xlsx)에 저장된 전 시트 측정 데이터를 채워 단일 엑셀 파일로 반환합니다.")
	@PostMapping(value = "/{scheduleId}/report/export", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<byte[]> exportReport(
		@PathVariable Long scheduleId,
		@RequestParam("template") MultipartFile template,
		@AuthenticationPrincipal CustomUserDetails principal
	) throws IOException {
		byte[] result = exportService.exportReport(scheduleId, principal.getTenantId(), template.getBytes());

		return ResponseEntity.ok()
			.contentType(XLSX)
			.header(HttpHeaders.CONTENT_DISPOSITION, attachment("성적서-" + scheduleId + ".xlsx"))
			.body(result);
	}

	@Operation(summary = "채취기록부 다운로드(시트별 ZIP)",
		description = "업로드한 jxls 템플릿(단일 시트 바인딩)에 측정 시트별 데이터를 채워, 시트당 파일 하나씩을 ZIP으로 묶어 반환합니다.")
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

	/** Content-Disposition 헤더값 생성(파일명 UTF-8 인코딩). */
	private String attachment(String filename) {
		String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
		return "attachment; filename*=UTF-8''" + encoded;
	}
}
