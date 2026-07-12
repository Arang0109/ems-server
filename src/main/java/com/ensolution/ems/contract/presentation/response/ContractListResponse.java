package com.ensolution.ems.contract.presentation.response;

import java.time.LocalDate;

public record ContractListResponse(
	Long id,
	Long workplaceId,
	String contractName,
	String clientName,
	String workplaceName,
	LocalDate contractDate,
	long taskPeriod,
	String fields
) {}
