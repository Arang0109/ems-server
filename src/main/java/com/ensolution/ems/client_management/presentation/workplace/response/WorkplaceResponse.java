package com.ensolution.ems.client_management.presentation.workplace.response;

public record WorkplaceResponse(
	Long id,
	Long clientId,
	String name,
	String zipcode,
	String roadAddress,
	String address,
	String bizNumber
) {}