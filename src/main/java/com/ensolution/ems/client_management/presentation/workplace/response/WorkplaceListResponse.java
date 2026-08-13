package com.ensolution.ems.client_management.presentation.workplace.response;

import com.ensolution.ems.global.common.enums.Grade;

public record WorkplaceListResponse(
	Long id,
	Long clientId,
	String clientName,
	String workplaceName,
	String bizNumber,
	String businessCategory,
	String roadAddress,
	String detailAddress,
	String zipcode,
	String facilityManager,
	String samplingWitness,
	Grade grade
) { }
