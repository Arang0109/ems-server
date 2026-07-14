package com.ensolution.ems.tenant.presentation.workplace.response;

public record WorkplaceResponse(
	Long id,
	Long clientId,
	String name,
	String roadAddress,
	String detailAddress,
	String zipcode,
	String bizNumber
) {}