package com.ensolution.ems.tenant.presentation.workplace.response;

public record WorkplaceListResponse(
	Long id,
	Long clientId,
	String clientName,
	String workplaceName,
	String bizNumber,
	String roadAddress,
	String detailAddress,
	String zipcode
) { }
