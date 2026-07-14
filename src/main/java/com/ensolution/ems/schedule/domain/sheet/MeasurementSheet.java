package com.ensolution.ems.schedule.domain.sheet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.util.List;

/**
 * 측정 시트. 하나의 측정 카테고리(가스/중금속/먼지/수은)에 대한 측정값과 계산 결과를 담는다.
 * 산소보정계수 계산 입력인 표준산소농도는 측정시설(Stack) 원장 속성이므로 시트가 아니라 스냅샷에서 취한다.
 * {@code avgTg}·{@code avgPv}·{@code avgPs}·{@code avgTm}·{@code quantity}는 계산 결과 필드다.
 */
@Getter
@Jacksonized
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class MeasurementSheet {

	private MeasurementCategory category;

	private WeatherData weather;
	private MoistureData moisture;
	private ExhaustGasData exhaustGas;

	private List<MeasurementPoint> measurementPoints;
	private List<Sample> samples;
	private ParticleSample particleSample;

	// 계산 결과
	private BigDecimal avgTg; // 배출가스 절대온도 (K)
	private BigDecimal avgPv; // 배출가스 평균 동압
	private BigDecimal avgPs; // 배출가스 평균 정압
	private BigDecimal avgTm; // 가스미터 절대온도 (K)
	private BigDecimal quantity; // 유량
}
