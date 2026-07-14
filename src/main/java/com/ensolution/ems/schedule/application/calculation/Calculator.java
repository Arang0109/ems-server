package com.ensolution.ems.schedule.application.calculation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** 측정 계산 공용 유틸. */
@Component
public class Calculator {

	/** null 값은 그대로 null 반환(저장 시 부분 계산을 허용). */
	public BigDecimal round(BigDecimal value, int scale) {
		return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
	}

	/** null은 0으로 취급해 평균. 비어 있으면 0. */
	public BigDecimal averageTreatNullAsZero(List<BigDecimal> values, int scale) {
		if (values == null || values.isEmpty()) return BigDecimal.ZERO;

		return values.stream()
			.map(v -> v == null ? BigDecimal.ZERO : v)
			.reduce(BigDecimal.ZERO, BigDecimal::add)
			.divide(BigDecimal.valueOf(values.size()), scale, RoundingMode.HALF_UP);
	}
}
