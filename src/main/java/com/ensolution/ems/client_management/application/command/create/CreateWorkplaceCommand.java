package com.ensolution.ems.client_management.application.command.create;

import com.ensolution.ems.global.common.enums.Grade;

public record CreateWorkplaceCommand(
	Long tenantId,
	Long clientId,
	String name,
	String roadAddress,
	String detailAddress,
	String zipcode,
	String bizNumber,
	String facilityManager,
	String samplingWitness,
	Grade grade
) {
}
