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
 * 유량 집계는 {@link QuantityData}, 입자상 집계는 {@link ParticleData}, 측정점별 원시데이터는 {@link SamplingPoint}에 있다.
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

	// 유량 집계 (항상)
	private QuantityData quantity;
	// 입자상 집계 (입자상 시트에만 존재)
	private ParticleData particle;

	// 측정점별 원시 데이터 (유량 항상 + 입자상 nullable 중첩)
	private List<SamplingPoint> samplingPoints;

	private List<Sample> samples;

	// 계산 결과
	private Integer samplingPointCnt;
	private BigDecimal avgTm; // 가스미터 절대온도 (K)
}
