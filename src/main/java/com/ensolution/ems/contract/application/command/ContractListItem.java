package com.ensolution.ems.contract.application.command;

import com.ensolution.ems.contract.domain.ContractAmountUnit;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractListItem(
	Long id,
	Long workplaceId,
	String contractName,
	LocalDate contractDate,
	LocalDate startDate,
	LocalDate completionDate,
	BigDecimal contractAccount,
	ContractAmountUnit contractAmountUnit
) {}
