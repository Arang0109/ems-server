package com.ensolution.ems.schedule.domain.sheet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 입자상 물질 채취 집계 결과. 측정점별 등속흡인 원시데이터는 {@link SamplingPoint.ParticleSampling}에 있고,
 * 여기에는 시트 단위 집계값만 담는다. 입자상(먼지/중금속/수은) 시트에서만 존재한다.
 */
@Getter
@Jacksonized @Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ParticleData {

	// 계산 결과
	private BigDecimal avgKFactor;
	private BigDecimal avgOrificeDp;
	private BigDecimal avgIsokineticRatio;
	private BigDecimal totalVm; // 총 건식가스미터 채취량
	private BigDecimal totalSamplingTime;
	private LocalTime samplingStartTime;
	private LocalTime samplingEndTime;

	private String thimbleFilter;
	private String bgThimbleFilter;
}
