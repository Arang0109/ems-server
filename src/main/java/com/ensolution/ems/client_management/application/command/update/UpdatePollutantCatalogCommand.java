package com.ensolution.ems.client_management.application.command.update;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;

/** {@code code}는 포함하지 않는다 — 프론트 분기와 기존 측정계획 스냅샷이 의존하므로 변경을 허용하지 않는다. */
public record UpdatePollutantCatalogCommand(
	MeasurementField field,
	String nameKr,
	MeasurementMethod method,
	PollutantPhase phase,
	Integer sortOrder
) {}
