package com.ensolution.ems.schedule.presentation.request;

import com.ensolution.ems.global.common.enums.MeasurementField;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateScheduleRequest(
	@NotNull(message = "측정 대상 시설은 필수 값입니다.")
	Long stackId,
	@NotNull(message = "측정 팀은 필수 값입니다.")
	Long teamId,
	@NotNull(message = "측정 분야는 필수 값입니다.")
	MeasurementField measurementField,
	@NotNull(message = "측정 일시는 필수 값입니다.")
	LocalDateTime measureDate,
	String measurementType,
	String referenceNumber
) {}
