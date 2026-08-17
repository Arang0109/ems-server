package com.ensolution.ems.client_management.application.command.list_item;

import com.ensolution.ems.client_management.domain.PollutantSource;
import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;

/**
 * 이 tenant가 선택할 수 있는 측정물질 한 건. 지원 물질 가이드와 이 tenant의 채택 현황을 합친 값이다.
 *
 * @param pollutantId 채택해 보유 중인 행 id. {@code CATALOG}(아직 채택하지 않음)이면 null
 * @param catalogId   가이드 항목 id. 항상 채워진다
 * @param code        전역 불변 키. 프론트가 특정 물질을 판별할 때 쓴다. 항상 채워진다
 * @param nameEn      고객사 소유값. {@code CATALOG}이면 아직 없으므로 null
 * @param equipment   고객사 소유값. {@code CATALOG}이면 아직 없으므로 null
 * @param testMethod  고객사 소유값. {@code CATALOG}이면 아직 없으므로 null
 */
public record PollutantListItem(
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
	String testMethod,
	Integer sortOrder
) {}
