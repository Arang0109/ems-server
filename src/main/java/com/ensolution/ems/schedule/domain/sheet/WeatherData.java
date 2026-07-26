package com.ensolution.ems.schedule.domain.sheet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 날씨 정보. {@code Pa}(대기압 mmHg)는 계산 파이프라인이 채우는 결과 필드. */
@Getter
@Jacksonized @Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class WeatherData {
	
	private BigDecimal pressure;	// 대기압 (Hpa)
	private WeatherCondition weatherCondition;
	private BigDecimal temperature;
	private BigDecimal humidity;
	private WindDirection windDirection;
	private BigDecimal windSpeed;

	// 계산 결과
	private BigDecimal Pa; // 대기압 (mmHg)
}
