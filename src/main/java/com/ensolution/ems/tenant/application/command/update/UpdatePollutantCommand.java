package com.ensolution.ems.tenant.application.command.update;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;

public record UpdatePollutantCommand(
	MeasurementField field,
	String nameKr,
	String nameEn,
	MeasurementMethod method,
	PollutantPhase phase,
	String equipment,
	String testMethod
) {}
