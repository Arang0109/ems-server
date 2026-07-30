package com.ensolution.ems.client_management.application.command.update;

import com.ensolution.ems.global.common.enums.Grade;

public record UpdateWorkplaceCommand(
	String name,
	String bizNumber,
	String roadAddress,
	String detailAddress,
	String zipcode,
	String facilityManager,
	String samplingWitness,
	Grade grade
) {
}
