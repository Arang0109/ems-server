package com.ensolution.ems.client_management.application.command.create;

import com.ensolution.ems.global.common.enums.Grade;

public record CreateWorkplaceCommand(
	Long companyId,
	String name,
	String address,
	String bizNumber,
	Grade grade
) {
}
