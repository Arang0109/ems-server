package com.ensolution.ems.dashboard.presentation.response;

public record ExpiringContractResponse(
	String contractName,
	long daysRemaining
) {
}