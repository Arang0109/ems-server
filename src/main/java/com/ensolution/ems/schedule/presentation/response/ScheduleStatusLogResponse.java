package com.ensolution.ems.schedule.presentation.response;

import com.ensolution.ems.schedule.domain.ScheduleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** 측정계획 상태 변경 이력 응답. */
public record ScheduleStatusLogResponse(
	Long id,

	@Schema(description = "직전 상태. 최초 등록 이력은 null입니다.")
	ScheduleStatus fromStatus,

	@Schema(description = "변경된 상태")
	ScheduleStatus toStatus,

	@Schema(description = "변경 사유. 취소·재개방은 항상 존재하고 자동 전이는 null입니다.")
	String reason,

	@Schema(description = "서버가 스냅샷 입력을 보고 전진시킨 자동 전이 여부")
	boolean automatic,

	@Schema(description = "변경한 사용자 id. 자동 전이는 null입니다.")
	Long changedBy,

	LocalDateTime changedAt
) {}
