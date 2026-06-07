package com.ensolution.ems.contract.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Contract {
	private Long id;
	private Long workplaceId;
	private String contractName;
	private LocalDate contractDate;
	private LocalDate startDate;
	private LocalDate completionDate;
	private BigDecimal contractAmount;
	private ContractAmountUnit contractAmountUnit;
	private boolean vatIncluded;
	private BigDecimal contractGuaranteeAmount;
	private BigDecimal advancePaymentAmount;
	private int advancePaymentDueDate;
	private int delayPenaltyRate;
	private String remark;

	public static Contract register(
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
		String remark
	) {
		return Contract.builder()
			.workplaceId(workplaceId)
			.contractName(contractName)
			.contractDate(contractDate)
			.startDate(startDate)
			.completionDate(completionDate)
			.contractAmount(contractAmount)
			.contractAmountUnit(contractAmountUnit)
			.vatIncluded(vatIncluded)
			.contractGuaranteeAmount(contractGuaranteeAmount)
			.advancePaymentAmount(advancePaymentAmount)
			.advancePaymentDueDate(advancePaymentDueDate)
			.delayPenaltyRate(delayPenaltyRate)
			.remark(remark)
			.build();
	}

	public Contract update(
		Long contractId,
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
	) {
		return this.toBuilder()
			.id(contractId)
			.contractName(keep(contractName, this.contractName))
			.contractDate(contractDate != null ? contractDate : this.contractDate)
			.startDate(startDate != null ? startDate : this.startDate)
			.completionDate(completionDate != null ? completionDate : this.completionDate)
			.contractAmount(contractAmount != null ? contractAmount : this.contractAmount)
			.contractAmountUnit(contractAmountUnit != null ? contractAmountUnit : this.contractAmountUnit)
			.vatIncluded(vatIncluded != null ? vatIncluded : this.vatIncluded)
			.contractGuaranteeAmount(contractGuaranteeAmount != null ? contractGuaranteeAmount : this.contractGuaranteeAmount)
			.advancePaymentAmount(advancePaymentAmount != null ? advancePaymentAmount : this.advancePaymentAmount)
			.advancePaymentDueDate(advancePaymentDueDate != null ? advancePaymentDueDate : this.advancePaymentDueDate)
			.delayPenaltyRate(delayPenaltyRate != null ? delayPenaltyRate : this.delayPenaltyRate)
			.remark(keep(remark, this.remark))
			.build();
	}

	private static String keep(String value, String original) {
		return value == null || value.isBlank() ? original : value;
	}
}
