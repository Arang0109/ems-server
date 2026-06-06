package com.ensolution.ems.contract.presentation.response;

import com.ensolution.ems.global.common.enums.MeasurementField;

import java.time.LocalDate;
import java.util.List;

public record ContractListResponse(
	Long id,
	String contractName,
	LocalDate contractDate,
	String companyName,
	String workplaceName,
	List<MeasurementField> fieldList
) {}
