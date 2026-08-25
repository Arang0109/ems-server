package com.ensolution.ems.client_management.application.port.in;

import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.global.common.enums.MeasurementField;

import java.math.BigDecimal;

/**
 * 시설별 측정항목을 사업장·측정시설 이름과 함께 펼친 평면 요약.
 * 주기 이행 현황판의 <b>행 축</b>으로 쓰인다 — 한 번도 측정하지 않은 항목도 행으로 나와야 하므로,
 * 축은 이행 이력이 아니라 원장인 이 모듈에서 와야 한다.
 *
 * <p>{@link StackMeasurementSummary}(측정 시점 스냅샷용 트리)와 달리 트리를 만들지 않고 목록만 돌려준다.
 * 현황판은 테넌트 전체를 한 번에 훑으므로 시설별 트리 조회를 반복하면 N+1이 된다.
 *
 * @param code 측정물질 가이드 키. 카탈로그 도입 이전에 채택된 물질은 null이므로 소비처는 이름으로 폴백한다
 */
public record StackMeasurementItemSummary(
	Long workplaceId,
	String workplaceName,
	Long stackId,
	String stackName,
	MeasurementField field,
	Long stackPollutantId,
	Long pollutantId,
	String code,
	String nameKr,
	MeasurementCycle cycle,
	BigDecimal allowance
) {}
