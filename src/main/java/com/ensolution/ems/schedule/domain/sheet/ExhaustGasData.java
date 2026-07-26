package com.ensolution.ems.schedule.domain.sheet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * 배출가스 정보. 농도는 측정점별 다중값(List)이며, {@code gasDensity}·{@code o2CorrectionFactor}는 계산 결과 필드.
 */
@Getter
@Jacksonized @Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ExhaustGasData {

	private List<BigDecimal> o2Concentration;
	private List<BigDecimal> co2Concentration;
	private List<BigDecimal> coConcentration;
	
	private List<BigDecimal> noxConcentration;
	private List<BigDecimal> soxConcentration;
	
	private LocalTime gasAnalyzerStartTime;	// 가스분석기 측정시작시간 (15분 고정)
	private LocalTime thcAnalyzerStartTime; // THC분석기 측정시작시간 (30분 고정)

	// 계산 결과
	private BigDecimal standardGasDensity;					// 표준상태 배출가스밀도
	private BigDecimal o2CorrectionFactor; 					// 산소보정계수
}
