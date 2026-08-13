package com.ensolution.ems.dashboard.application.command;

import java.time.LocalDate;

/** 만료 임박 계약 항목. {@code daysRemaining}은 조회 기준일부터 완료일까지의 잔여일수다. */
public record ExpiringContract(
	Long contractId,
	String contractName,
	String workplaceName,
	LocalDate completionDate,
	long daysRemaining
) {
}
