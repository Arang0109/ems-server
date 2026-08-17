package com.ensolution.ems.client_management.infrastructure.bootstrap;

import com.ensolution.ems.global.common.enums.MeasurementField;
import com.ensolution.ems.global.common.enums.MeasurementMethod;
import com.ensolution.ems.global.common.enums.PollutantPhase;

/** 카탈로그 시드 파일의 한 항목. 카탈로그가 보유하는 필드와 1:1로 대응한다. */
public record PollutantCatalogSeedItem(
	String code,
	MeasurementField field,
	String nameKr,
	MeasurementMethod method,
	PollutantPhase phase,
	Integer sortOrder
) {}
