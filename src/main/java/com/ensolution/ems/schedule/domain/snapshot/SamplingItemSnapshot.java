package com.ensolution.ems.schedule.domain.snapshot;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;

import java.math.BigDecimal;

/**
 * 측정 시점 측정항목 스냅샷(시설별 측정물질 + 측정물질 마스터 결합).
 *
 * @param code 전역 측정물질 카탈로그 키(예: {@code NOX}). 카탈로그 도입 이전에 생성된 스냅샷과
 *             고객사 자체 물질은 null이므로, 소비처는 null을 허용하고 {@code nameKr}로 폴백해야 한다
 */
public record SamplingItemSnapshot(
	Long stackPollutantId,
	Long pollutantId,
	String code,
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
