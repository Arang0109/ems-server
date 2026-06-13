package com.ensolution.ems.client_management.presentation.workplace;

public record WorkplaceTableResponse(
	Long id,
	Long companyId,
	String companyName,
	String workplaceName,
	String bizNumber,
	String address
) { }
