package com.ensolution.ems.dashboard.presentation.response;

import java.time.LocalDate;

/** 만료 임박 계약 응답 항목. */
public record ExpiringContractResponse(
	Long contractId,
	String contractName,
	String workplaceName,
	LocalDate completionDate,
	long daysRemaining
) {
}
