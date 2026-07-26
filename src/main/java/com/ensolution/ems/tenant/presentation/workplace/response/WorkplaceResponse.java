package com.ensolution.ems.tenant.presentation.workplace.response;

import com.ensolution.ems.global.common.enums.Grade;

public record WorkplaceResponse(
	Long id,
	Long clientId,
	String name,
	String roadAddress,
	String detailAddress,
	String zipcode,
	String bizNumber,
	Grade grade
) {}