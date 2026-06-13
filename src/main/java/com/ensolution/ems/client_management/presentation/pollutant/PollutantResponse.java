package com.ensolution.ems.client_management.presentation.pollutant;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;

public record PollutantResponse(
	Long id,
	MeasurementField field,
	String nameKr,
	String nameEn,
	MeasurementMethod method,
	PollutantPhase phase,
	String equipment,
	String testMethod
) {}
