package com.ensolution.ems.schedule.domain.sampling;

import com.ensolution.ems.global.common.enums.MeasurementMode;
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

	/**
	 * 등속흡인 방식의 측정항목이 채취되는 입자상 기록지. 먼지·중금속·수은 기록지는 그 방식의 등속흡인 트레인
	 * 자체이므로 이름이 같다. 등속흡인이 아닌 방식(현장측정·가스상 채취)은 입자상 기록지가 없어 null이다.
	 *
	 * <p>등속흡인 항목이 담긴 가스상 시료 행(비소화합물 흡수액)이 어느 기록지에 적혔든 그 행의 출처는
	 * 이 카테고리 기록지의 입자상 집계다 — 행을 다른 기록지로 옮겨도 출처가 바뀌지 않는다.
	 */
	public static MeasurementCategory particulateSourceOf(MeasurementMode mode) {
		if (mode == null) return null;
		return switch (mode) {
			case DUST -> DUST;
			case HEAVY_METAL -> HEAVY_METAL;
			case MERCURY -> MERCURY;
			case DIRECT_READING, GAS_SAMPLING -> null;
		};
	}
}
