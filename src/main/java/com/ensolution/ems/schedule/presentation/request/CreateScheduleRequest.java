package com.ensolution.ems.schedule.presentation.request;

import com.ensolution.ems.global.common.enums.MeasurementField;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CreateScheduleRequest(
	@NotNull(message = "측정 대상 시설은 필수 값입니다.")
	Long stackId,
	@NotNull(message = "측정 팀은 필수 값입니다.")
	Long teamId,
	@NotNull(message = "측정 분야는 필수 값입니다.")
	MeasurementField measurementField,
	@NotNull(message = "측정 일자는 필수 값입니다.")
	LocalDate sampledAt,
	@NotEmpty(message = "측정 항목은 하나 이상 선택해야 합니다.")
	List<Long> pollutantIds,
	String schedulePurpose,
	String referenceNumber
) {}
