package com.ensolution.ems.client_management.presentation.response;

import com.ensolution.ems.global.common.enums.MeasurementField;

import java.time.LocalDateTime;

public record StackListResponse(
	Long id,
	String companyName,
	String workplaceName,
	MeasurementField field,
	String stackName,
	LocalDateTime createdAt,
	LocalDateTime modifiedAt
) { }
