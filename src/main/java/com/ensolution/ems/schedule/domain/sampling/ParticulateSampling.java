package com.ensolution.ems.schedule.domain.sampling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 입자상 물질 채취 집계 결과. 측정점별 등속흡인 원시데이터는 {@link SamplingPoint.IsokineticSamplingData}에 있고,
 * 여기에는 시트 단위 집계값만 담는다. 입자상(먼지/중금속/수은) 시트에서만 존재한다.
 *
 * <p>{@code averageGasMeterTemperature}는 측정점별 가스미터 온도의 평균이다. 시트가 직접 들고
 * 있었으나 나머지 집계값과 성격이 같아 여기로 모았다 — 성적서의 {@code particle.avgMeterTemperatureK}
 * 칸이 이 값을 쓴다.
 */
@Getter
@Jacksonized @Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ParticulateSampling {

	// 계산 결과
	private BigDecimal averageKFactor;
	private BigDecimal averageOrificeDifferentialPressure;
	private BigDecimal averageIsokineticRatio;
	private BigDecimal appliedNozzleDiameter;
	private BigDecimal nozzleArea;
	
	private BigDecimal averageGasMeterTemperature; // 가스미터 평균 절대온도 (K)
	private BigDecimal totalDryGasVolume; // 총 건식가스미터 채취량
	private BigDecimal totalSamplingTime;
	
	private LocalTime samplingStartedAt;
	private LocalTime samplingEndedAt;

	private String thimbleFilter;
	private String bgThimbleFilter;
}
