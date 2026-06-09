package com.ensolution.ems.client_management.presentation.response.table;

import com.ensolution.ems.global.common.enums.MeasurementField;

import java.time.LocalDateTime;

public record StackTableResponse(
	Long id,
	String companyName,
	String workplaceName,
	MeasurementField field,
	String stackName,
	LocalDateTime createdAt,
	LocalDateTime modifiedAt
) { }
