package com.ensolution.ems.schedule.domain.sheet;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 측정 카테고리. 측정 시트 특화 개념이므로 schedule 모듈 내부에 둔다. */
@Getter
@RequiredArgsConstructor
public enum MeasurementCategory {
	GAS("가스상"),
	HEAVY_METAL("중금속"),
	DUST("먼지"),
	MERCURY("수은");

	private final String description;
}
