package com.ensolution.ems.schedule.presentation.request;

import com.ensolution.ems.schedule.domain.ScheduleStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeScheduleStatusRequest(
	@NotNull(message = "변경할 측정계획 상태는 필수 값입니다.")
	ScheduleStatus status
) {}
