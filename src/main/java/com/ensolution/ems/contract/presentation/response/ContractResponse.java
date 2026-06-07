package com.ensolution.ems.contract.presentation.response;

import com.ensolution.ems.contract.domain.ContractAmountUnit;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractResponse(
	Long id,
	Long workplaceId,
	String contractName,
	LocalDate contractDate,
	LocalDate startDate,
	LocalDate completionDate,
	BigDecimal contractAmount,
	ContractAmountUnit contractAmountUnit,
	boolean vatIncluded,
	BigDecimal contractGuaranteeAmount,
	BigDecimal advancePaymentAmount,
	int advancePaymentDueDate,
	int delayPenaltyRate,
	String remark,
	String companyName,
	String workplaceName,
	String workplaceAddress
) {}
