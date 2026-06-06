package com.ensolution.ems.contract.presentation.response;

import com.ensolution.ems.contract.domain.ContractAmountUnit;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractListResponse(
	Long id,
	Long workplaceId,
	String contractName,
	LocalDate contractDate,
	LocalDate startDate,
	LocalDate completionDate,
	BigDecimal contractAccount,
	ContractAmountUnit contractAmountUnit
) {}
