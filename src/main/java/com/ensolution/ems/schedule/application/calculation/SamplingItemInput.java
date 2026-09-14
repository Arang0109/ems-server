package com.ensolution.ems.schedule.application.calculation;

import com.ensolution.ems.schedule.domain.sampling.MeasurementCategory;

/**
 * 가스상 시료 행의 파생값 계산에 필요한 측정항목 입력. 계산 엔진은 스냅샷을 모르므로
 * {@code SnapshotSheetReCalculator}가 {@code SamplingItemSnapshot}에서 이 둘만 추려 넘긴다({@link StackData}와 같은 이음매).
 *
 * @param pollutantId        행의 {@code GaseousSampling.pollutantIds}와 잇는 키
 * @param particulateSource  등속흡인 트레인으로 채취하는 항목(카탈로그 측정방식이 먼지·중금속·수은)이면 그 입자상 기록지의
 *                           카테고리, 아니면 null. 이 항목이 담긴 행은 어느 기록지에 적혔든 시각·유량·채취량을
 *                           그 카테고리 기록지의 입자상 집계에서 파생한다
 * @param samplingMinutes    이 항목에 적용되는 표준 채취시간(분). 시작시각만 적힌 행의 종료시각을 채우는 기본값이며,
 *                           정해지지 않은 항목은 null
 */
public record SamplingItemInput(
	Long pollutantId,
	MeasurementCategory particulateSource,
	Integer samplingMinutes
) {
	public boolean isIsokinetic() {
		return particulateSource != null;
	}
}
