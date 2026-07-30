package com.ensolution.ems.client_management.application.port.in;

public record ContractSummary(
	String clientName,
	String workplaceName,
	String address
) {
}
