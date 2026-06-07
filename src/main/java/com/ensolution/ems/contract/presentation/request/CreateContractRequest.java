package com.ensolution.ems.contract.presentation.request;

import com.ensolution.ems.contract.domain.ContractAmountUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateContractRequest(
	@NotNull Long workplaceId,
	@NotBlank String contractName,
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
	String remark
) {}
