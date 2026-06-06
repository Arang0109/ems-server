package com.ensolution.ems.contract.application.command;

import com.ensolution.ems.contract.domain.ContractAmountUnit;
import com.ensolution.ems.global.common.enums.MeasurementField;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ContractListItem(
	Long id,
	Long workplaceId,
	String contractName,
	LocalDate contractDate,
	LocalDate startDate,
	LocalDate completionDate,
	BigDecimal contractAccount,
	ContractAmountUnit contractAmountUnit,
	String companyName,
	String workplaceName,
	List<MeasurementField> fieldList
) {
	public ContractListItem withFieldList(List<MeasurementField> fieldList) {
		return new ContractListItem(id, workplaceId, contractName, contractDate, startDate,
			completionDate, contractAccount, contractAmountUnit, companyName, workplaceName, fieldList);
	}
}
