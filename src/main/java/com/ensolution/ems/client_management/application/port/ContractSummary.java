package com.ensolution.ems.client_management.application.port;

import com.ensolution.ems.global.common.enums.MeasurementField;

import java.util.List;

public record ContractSummary(
	List<MeasurementField> fieldList,
	String companyName,
	String workplaceName
) {
}
