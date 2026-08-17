package com.ensolution.ems.client_management.presentation.pollutant_catalog.request;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;

/**
 * {@code code}는 수정 대상이 아니다 — 클라이언트 분기와 기존 측정계획 스냅샷이 이 값에 의존한다.
 *
 * <p>{@code nameKr} 수정은 앞으로 채택할 고객사에만 반영된다. 이미 채택한 고객사는 자신의 표기명을 직접 보유한다.
 */
public record UpdatePollutantCatalogRequest(
	MeasurementField field,
	String nameKr,
	MeasurementMethod method,
	PollutantPhase phase,
	Integer sortOrder
) {}
