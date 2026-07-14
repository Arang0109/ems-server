package com.ensolution.ems.tenant.application.command.update;

import com.ensolution.ems.global.common.enums.Grade;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.Orientation;
import com.ensolution.ems.global.common.enums.Shape;

public record UpdateStackCommand(
	MeasurementField field,
	String name,
	String semsNumber,
	Grade grade,
	String businessCategory,
	String mainProduct,
	Integer standardOxygen,
	String height,
	String horizontalLength,
	String verticalLength,
	Shape shape,
	Orientation orientation
) {
}
