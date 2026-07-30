package com.ensolution.ems.client_management.application.command.create;

import com.ensolution.ems.global.common.enums.Grade;
import com.ensolution.ems.global.common.enums.MeasurementField;

public record CreateStackCommand(
	Long tenantId,
	Long workplaceId,
	MeasurementField field,
	String name,
	String semsNumber,
	Grade grade,
	String businessCategory,
	String mainProduct
) {
}
