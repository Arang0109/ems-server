package com.ensolution.ems.contract.application.command;

import com.ensolution.ems.contract.domain.ContractAmountUnit;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateContractCommand(
	Long workplaceId,
	String contractName,
	LocalDate contractDate,
	LocalDate startDate,
	LocalDate completionDate,
	BigDecimal contractAccount,
	ContractAmountUnit contractAmountUnit,
	boolean vatIncluded,
	BigDecimal contractGuaranteeAmount,
	BigDecimal advancePaymentAmount,
	int advancePaymentDueDate,
	int delayPenaltyRate,
	String remark
) {}
