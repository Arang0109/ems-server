package com.ensolution.ems.contract.application.command;

import com.ensolution.ems.contract.domain.ContractAmountUnit;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateContractCommand(
	String contractName,
	LocalDate contractDate,
	LocalDate startDate,
	LocalDate completionDate,
	BigDecimal contractAmount,
	ContractAmountUnit contractAmountUnit,
	Boolean vatIncluded,
	BigDecimal contractGuaranteeAmount,
	BigDecimal advancePaymentAmount,
	Integer advancePaymentDueDate,
	Integer delayPenaltyRate,
	String remark
) {}
