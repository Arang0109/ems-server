package com.ensolution.ems.client_management.application.command.update;

import com.ensolution.ems.global.common.enums.Grade;

public record UpdateWorkplaceCommand(
	String name,
	String address,
	String bizNumber,
	Grade grade
) {
}
