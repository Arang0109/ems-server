package com.ensolution.ems.client_management.presentation.pollutant.response;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.PollutantPhase;

/**
 * 아직 채택하지 않은 지원 물질 가이드 항목. 측정물질 등록 화면의 선택 후보다.
 *
 * <p>영문명·시험장비·시험방법은 가이드가 보유하지 않는다 — 채택한 뒤 고객사가 직접 입력하는 값이다.
 * 측정방법도 가이드가 갖지 않는다 — 같은 물질이라도 업체마다 다를 수 있어 채택할 때 고객사가 지정한다.
 *
 * @param catalogId 채택할 때 지목하는 id. code는 측정분야 안에서만 유일하므로 id를 쓴다
 * @param code      모든 고객사에서 동일한 불변 키(예: {@code NOX}). 화면 안에서 물질을 분기할 때 쓴다
 * @param sortOrder 법령 고시 순서
 */
public record PollutantCandidateResponse(
	Long catalogId,
	String code,
	MeasurementField field,
	String nameKr,
	PollutantPhase phase,
	Integer sortOrder
) {}
