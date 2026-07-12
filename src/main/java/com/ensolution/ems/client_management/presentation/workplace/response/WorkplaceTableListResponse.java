package com.ensolution.ems.client_management.presentation.workplace.response;

public record WorkplaceTableListResponse(
	Long id,
	Long clientId,
	String clientName,
	String workplaceName,
	String zipcode,
	String roadAddress,
	String bizNumber,
	String address
) { }
