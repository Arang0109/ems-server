package com.ensolution.ems.contract.infrastructure;

import com.ensolution.ems.contract.domain.ContractAmountUnit;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractQueryRow(
	Long id,
	Long workplaceId,
	String contractName,
	LocalDate contractDate,
	LocalDate startDate,
	LocalDate completionDate,
	BigDecimal contractAccount,
	ContractAmountUnit contractAmountUnit,
	String companyName,
	String workplaceName
) {}
