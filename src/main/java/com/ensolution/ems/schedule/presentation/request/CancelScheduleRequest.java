package com.ensolution.ems.schedule.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 측정계획 취소 요청. 취소는 업무 이력으로 남으므로 사유가 필수다. */
public record CancelScheduleRequest(
	@Schema(description = "취소 사유", example = "의뢰기관 요청으로 측정 일정 취소")
	@NotBlank(message = "취소 사유를 입력해 주세요.")
	@Size(max = 500, message = "취소 사유는 500자를 넘을 수 없습니다.")
	String reason
) {}
