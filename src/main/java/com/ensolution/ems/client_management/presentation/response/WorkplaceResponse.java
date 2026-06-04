package com.ensolution.ems.client_management.presentation.response;

public record WorkplaceResponse(
	Long id,
	Long companyId,
	String name,
	String address,
	String bizNumber
) {}