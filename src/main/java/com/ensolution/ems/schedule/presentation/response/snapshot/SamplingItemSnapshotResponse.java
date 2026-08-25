package com.ensolution.ems.schedule.presentation.response.snapshot;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;

import java.math.BigDecimal;

/**
 * 측정 시점 측정항목 스냅샷 응답.
 * <p>
 * <b>배열 순서가 곧 성적서의 항목 순서다</b> — 기록부 서식이 인덱스로 칸을 지목하므로
 * 클라이언트는 이 순서를 임의로 정렬하지 않는다.
 *
 * @param code 전역 측정물질 카탈로그 키(예: {@code NOX}). 카탈로그 도입 이전 스냅샷과
 *             고객사 자체 물질은 null이므로 {@code nameKr}로 폴백해야 한다
 */
public record SamplingItemSnapshotResponse(
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
