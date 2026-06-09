package com.ensolution.ems.client_management.presentation.response.table;

public record WorkplaceTableResponse(
	Long id,
	Long companyId,
	String companyName,
	String workplaceName,
	String bizNumber,
	String address
) { }
