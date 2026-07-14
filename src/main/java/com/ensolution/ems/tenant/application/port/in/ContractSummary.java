package com.ensolution.ems.tenant.application.port.in;

public record ContractSummary(
	String clientName,
	String workplaceName,
	String address
) {
}
