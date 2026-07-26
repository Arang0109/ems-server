package com.ensolution.ems.schedule.application.command;

import com.ensolution.ems.global.common.enums.Orientation;
import com.ensolution.ems.global.common.enums.Shape;

import java.math.BigDecimal;

public record StackData(
	Shape stackShape,
	BigDecimal height,
	BigDecimal horizontalLength,
	BigDecimal verticalLength,
	Orientation orientation,
	BigDecimal standardOxygen
) {
}
