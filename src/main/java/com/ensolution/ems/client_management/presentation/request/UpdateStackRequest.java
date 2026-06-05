package com.ensolution.ems.client_management.presentation.request;

import com.ensolution.ems.global.common.enums.Grade;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.Orientation;
import com.ensolution.ems.global.common.enums.Shape;

public record UpdateStackRequest(
	MeasurementField field,
	String name,
	String semsNumber,
	Grade grade,
	String businessCategory,
	String mainProduct,
	String height,
	String horizontalLength,
	String verticalLength,
	Shape shape,
	Orientation orientation
) {}
