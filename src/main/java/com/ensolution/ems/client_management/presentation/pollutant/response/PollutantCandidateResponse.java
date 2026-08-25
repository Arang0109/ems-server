package com.ensolution.ems.client_management.presentation.pollutant.response;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;

/**
 * 아직 채택하지 않은 지원 물질 가이드 항목. 측정물질 등록 화면의 선택 후보다.
 *
 * <p>영문명·시험장비·시험방법은 가이드가 보유하지 않는다 — 채택한 뒤 고객사가 직접 입력하는 값이다.
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
	MeasurementMethod method,
	PollutantPhase phase,
	Integer sortOrder
) {}
