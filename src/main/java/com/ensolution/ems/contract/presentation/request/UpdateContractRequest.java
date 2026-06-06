package com.ensolution.ems.contract.presentation.request;

import com.ensolution.ems.contract.domain.ContractAmountUnit;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateContractRequest(
	String contractName,
	LocalDate contractDate,
	LocalDate startDate,
	LocalDate completionDate,
	BigDecimal contractAccount,
	ContractAmountUnit contractAmountUnit,
	Boolean vatIncluded,
	BigDecimal contractGuaranteeAmount,
	BigDecimal advancePaymentAmount,
	Integer advancePaymentDueDate,
	Integer delayPenaltyRate,
	String remark
) {}
