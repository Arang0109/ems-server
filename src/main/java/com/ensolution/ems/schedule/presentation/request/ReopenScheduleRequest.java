package com.ensolution.ems.schedule.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 완료된 측정계획의 재개방 요청. 완료를 되돌리는 예외 조치이므로 사유가 필수다. */
public record ReopenScheduleRequest(
	@Schema(description = "재개방 사유", example = "성적서 발행일자 오기입으로 재작성 필요")
	@NotBlank(message = "재개방 사유를 입력해 주세요.")
	@Size(max = 500, message = "재개방 사유는 500자를 넘을 수 없습니다.")
	String reason
) {}
