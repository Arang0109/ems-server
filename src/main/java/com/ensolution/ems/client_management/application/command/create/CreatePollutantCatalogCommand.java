package com.ensolution.ems.client_management.application.command.create;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.PollutantPhase;

public record CreatePollutantCatalogCommand(
	String code,
	MeasurementField field,
	String nameKr,
	PollutantPhase phase,
	Integer sortOrder
) {}
