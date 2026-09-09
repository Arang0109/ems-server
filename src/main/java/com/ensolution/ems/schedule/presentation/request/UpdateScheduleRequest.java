package com.ensolution.ems.schedule.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "측정계획 정의 수정 요청. 전달한 값을 그대로 채택하므로 빈 값은 기존 값을 지웁니다. "
	+ "측정분야와 측정 대상(측정시설·측정팀)은 생성 시점에만 정하며 이 경로로 바꿀 수 없습니다. "
	+ "시료접수·분석완료·성적서발행 일자는 PATCH /api/schedules/{scheduleId}/report-dates 로 수정합니다.")
public record UpdateScheduleRequest(
	@NotNull(message = "채취일자는 필수 값입니다.")
	@Schema(description = "채취(측정) 일자. 측정 건수 집계의 기준일이라 비울 수 없습니다.", example = "2026-05-01")
	LocalDate sampledAt,

	@Schema(description = "측정 용도", example = "자가측정용")
	String schedulePurpose,

	@Schema(description = "내부 식별 코드(관리번호)", example = "2026-A-001")
	String referenceNumber
) {}
