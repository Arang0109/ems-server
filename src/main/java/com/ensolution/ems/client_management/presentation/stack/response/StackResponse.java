package com.ensolution.ems.client_management.presentation.stack.response;

import com.ensolution.ems.global.common.enums.Grade;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.Orientation;
import com.ensolution.ems.global.common.enums.Shape;

import java.time.LocalDateTime;

public record StackResponse(
	Long id,
	Long workplaceId,
	MeasurementField field,
	String name,
	String semsNumber,
	Grade grade,
	String mainProduct,
	String height,
	String horizontalLength,
	String verticalLength,
	Shape shape,
	Orientation orientation,
	Integer standardOxygen,
	LocalDateTime createdAt,
	LocalDateTime modifiedAt
) {}