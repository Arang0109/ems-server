package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;

import java.math.BigDecimal;

/** 측정 시점 측정항목 스냅샷(시설별 측정물질 + 측정물질 마스터 결합). */
public record SamplingItemSnapshot(
	Long stackPollutantId,
	Long pollutantId,
	String nameKr,
	String nameEn,
	MeasurementField field,
	MeasurementMethod method,
	PollutantPhase phase,
	String equipment,
	String testMethod,
	MeasurementCycle cycle,
	BigDecimal allowance,
	boolean oxygenApplicable
) {}
