package com.ensolution.ems.client_management.presentation.pollutant_catalog.response;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;

public record PollutantCatalogResponse(
	Long id,
	String code,
	MeasurementField field,
	String nameKr,
	MeasurementMethod method,
	PollutantPhase phase,
	Integer sortOrder,
	boolean active
) {}
