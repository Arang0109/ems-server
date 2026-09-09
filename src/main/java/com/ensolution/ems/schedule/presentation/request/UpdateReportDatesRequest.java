package com.ensolution.ems.schedule.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "성적서 진행 일자 수정 요청. 실험·분석 탭이 이 셋을 단독으로 소유하므로 "
	+ "전달한 값을 그대로 채택합니다 — 빈 값은 기존 값을 지웁니다. "
	+ "채취일자 ≤ 시료접수일 ≤ 분석완료일 ≤ 성적서발행일 순서를 지켜야 합니다.")
public record UpdateReportDatesRequest(
	@Schema(description = "시료접수일자", example = "2026-05-02")
	LocalDate receivedAt,

	@Schema(description = "분석완료일자", example = "2026-05-04")
	LocalDate analyzedAt,

	@Schema(description = "성적서발행일자", example = "2026-05-08")
	LocalDate issuedAt
) {}
