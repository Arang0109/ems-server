package com.ensolution.ems.tenant.application.command.list_item;

import com.ensolution.ems.global.common.enums.MeasurementField;

import java.time.LocalDateTime;

public record StackListItem(
	Long id,
	String clientName,
	String workplaceName,
	MeasurementField field,
	String name,
	LocalDateTime createdAt,
	LocalDateTime modifiedAt
) {}
