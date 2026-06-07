package com.ensolution.ems.contract.application.command;

import java.time.LocalDate;

public record ContractListItem(
	Long id,
	Long workplaceId,
	String contractName,
	String companyName,
	String workplaceName,
	LocalDate contractDate,
	LocalDate startDate,
	LocalDate completionDate,
	String fields
) {
}
