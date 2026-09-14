package com.ensolution.ems.schedule.presentation.response.snapshot;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMode;
import com.ensolution.ems.global.common.enums.PollutantPhase;

import java.math.BigDecimal;

/**
 * 측정 시점 측정항목 스냅샷 응답.
 * <p>
 * <b>배열 순서가 곧 성적서의 항목 순서다</b> — 기록부 서식이 인덱스로 칸을 지목하므로
 * 클라이언트는 이 순서를 임의로 정렬하지 않는다.
 * <p>
 * 판정 근거({@code allowance}·{@code oxygenApplicable})와 실험실 분석 결과({@code analysis})가
 * 한 항목 안에 함께 있어 둘이 갈라질 일이 없다. {@code analysis}가 null이면 아직 분석 전이다.
 *
 * @param code   전역 측정물질 카탈로그 키(예: {@code NOX}). 카탈로그 도입 이전 스냅샷과
 *               고객사 자체 물질은 null이므로 {@code nameKr}로 폴백해야 한다
 * @param mode   측정방식 분류(카탈로그 전역 사실). 구 문서는 null
 * @param method 측정 시점 측정방법 사본. 측정방법이 정해지지 않은 레거시 항목은 null이며,
 *               그 경우 가스상 시료 행을 자동으로 만들 수 없다
 * @param samplingMinutes 이 항목에 적용되는 표준 채취시간(분). 항목별 오버라이드가 반영된 값이라
 *               {@code method.samplingMinutes}(방법 기본값)와 다를 수 있다. 구 문서는 null
 */
public record SamplingItemSnapshotResponse(
	Long stackPollutantId,
	Long pollutantId,
	String code,
	String nameKr,
	String nameEn,
	MeasurementField field,
	MeasurementMethodSnapshotResponse method,
	PollutantPhase phase,
	MeasurementMode mode,
	String equipment,
	String testMethod,
	Integer samplingMinutes,
	MeasurementCycle cycle,
	BigDecimal allowance,
	boolean oxygenApplicable,
	AnalysisResultResponse analysis
) {}
