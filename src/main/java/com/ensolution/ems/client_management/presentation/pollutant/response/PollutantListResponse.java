package com.ensolution.ems.client_management.presentation.pollutant.response;

import com.ensolution.ems.client_management.domain.PollutantSource;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;

/**
 * 선택 가능한 측정물질 한 건.
 *
 * @param pollutantId 이 tenant의 측정물질 id. {@code source=CATALOG}(아직 채택하지 않음)이면 null
 * @param code        모든 tenant에서 동일한 전역 키(예: {@code NOX}). 항상 채워진다
 * @param source      {@code CATALOG}(미채택 가이드 항목) / {@code REGISTERED}(채택해 보유 중)
 * @param nameEn      고객사 소유값. {@code source=CATALOG}이면 아직 없으므로 null
 * @param equipment   고객사 소유값. {@code source=CATALOG}이면 아직 없으므로 null
 * @param testMethod  고객사 소유값. {@code source=CATALOG}이면 아직 없으므로 null
 */
public record PollutantListResponse(
	Long pollutantId,
	Long catalogId,
	String code,
	PollutantSource source,
	MeasurementField field,
	String nameKr,
	String nameEn,
	MeasurementMethod method,
	PollutantPhase phase,
	String equipment,
	String testMethod
) {}
