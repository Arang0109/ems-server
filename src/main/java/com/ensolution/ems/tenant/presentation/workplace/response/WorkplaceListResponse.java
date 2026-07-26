package com.ensolution.ems.tenant.presentation.workplace.response;

import com.ensolution.ems.global.common.enums.Grade;

public record WorkplaceListResponse(
	Long id,
	Long clientId,
	String clientName,
	String workplaceName,
	String bizNumber,
	String roadAddress,
	String detailAddress,
	String zipcode,
	Grade grade
) { }
