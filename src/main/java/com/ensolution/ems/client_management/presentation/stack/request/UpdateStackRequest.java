package com.ensolution.ems.client_management.presentation.stack.request;

import com.ensolution.ems.global.common.enums.Grade;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.Orientation;
import com.ensolution.ems.global.common.enums.Shape;

public record UpdateStackRequest(
	MeasurementField field,
	String name,
	String semsNumber,
	Grade grade,
	String mainProduct,
	Integer standardOxygen,
	Double height,
	Double horizontalLength,
	Double verticalLength,
	Shape shape,
	Orientation orientation
) {}
