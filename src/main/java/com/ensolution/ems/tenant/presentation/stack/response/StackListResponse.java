package com.ensolution.ems.tenant.presentation.stack.response;

import com.ensolution.ems.global.common.enums.MeasurementField;

import java.time.LocalDateTime;

public record StackListResponse(
	Long id,
	String clientName,
	String workplaceName,
	MeasurementField field,
	String stackName,
	LocalDateTime createdAt,
	LocalDateTime modifiedAt
) { }
