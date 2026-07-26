package com.ensolution.ems.tenant.application.command.list_item;

import com.ensolution.ems.global.common.enums.Grade;

public record WorkplaceListItem(
	Long id,
	Long clientId,
	String clientName,
	String name,
	String bizNumber,
	String roadAddress,
	String detailAddress,
	String zipcode,
	Grade grade
) {}
