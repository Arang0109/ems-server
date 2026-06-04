package com.ensolution.ems.client_management.presentation.response;

public record WorkplaceListResponse(
	Long id,
	Long companyId,
	String companyName,
	String workplaceName,
	String bizNumber,
	String address
) { }
