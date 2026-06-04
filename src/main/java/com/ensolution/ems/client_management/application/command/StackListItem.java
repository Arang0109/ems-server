package com.ensolution.ems.client_management.application.command;

import com.ensolution.ems.global.common.enums.MeasurementField;

import java.time.LocalDateTime;

public record StackListItem(
	Long id,
	String companyName,
	String workplaceName,
	MeasurementField field,
	String name,
	LocalDateTime createdAt,
	LocalDateTime modifiedAt
) {}
