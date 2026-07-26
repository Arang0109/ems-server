package com.ensolution.ems.schedule.application.command.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * 측정 시트 하나에 대응하는 엑셀 뷰. 입력값과 계산값을 모두 노출한다(고객 양식에 자체 계산식이 있을 수 있으므로 원시 입력도 제공).
 * 유량은 항상, 입자상 집계는 입자상 시트일 때만 값이 채워진다(그 외 null).
 * 측정점별 값은 {@link PointExportView}, 시료 채취 정보는 {@link SampleExportView} 목록으로 노출한다.
 * jxls(JEXL)는 표준 getter로 프로퍼티를 해석하므로 record가 아니라 {@code @Getter} 클래스로 둔다.
 */
@Getter
@Builder
@AllArgsConstructor
public class SheetExportView {

	private final String category;          // 가스상/중금속/먼지/수은
	private final Integer samplingPointCount;

	// ===== 날씨 입력 =====
	private final BigDecimal pressureHpa;   // 대기압 원값 (hPa)
	private final String weatherCondition;  // 맑음/흐림/비/눈
	private final BigDecimal temperature;
	private final BigDecimal humidity;
	private final String windDirection;     // 풍향 (북/북북동 등)
	private final BigDecimal windSpeed;
	// ----- 날씨 계산 -----
	private final BigDecimal atmosphericPressure; // 대기압 Pa (mmHg)

	// ===== 수분 입력 =====
	private final BigDecimal moistureWeightBefore;        // 흡습병 무게 전 (g)
	private final BigDecimal moistureWeightAfter;         // 흡습병 무게 후 (g)
	private final BigDecimal moistureGasMeterInTemperature;  // 가스미터 입구온도
	private final BigDecimal moistureGasMeterOutTemperature; // 가스미터 출구온도
	private final BigDecimal moistureDryGasVolumeBefore;  // 건조가스 부피 전 (L)
	private final BigDecimal moistureDryGasVolumeAfter;   // 건조가스 부피 후 (L)
	private final BigDecimal moistureSuctionVelocity;     // 흡입속도
	private final BigDecimal moistureGasMeterGaugePressure;// 가스미터 게이지압 (mmH2O)
	// ----- 수분 계산 -----
	private final BigDecimal moistureRatio;               // Xw 수분량 (%)
	private final BigDecimal moistureAbsorbedMass;        // ma 흡습 수분질량 (g)
	private final BigDecimal moistureIntakeGasTemperature;// Tm_g 가스미터 흡입 가스온도
	private final BigDecimal moistureIntakeDryGasVolume;  // Vm_g 흡입 건조가스량 (L)
	private final BigDecimal moistureGaugePressureMmHg;   // Pm_g 가스미터 게이지압 (mmHg)

	// ===== 배출가스 입력 =====
	private final List<BigDecimal> o2Concentrations;
	private final List<BigDecimal> co2Concentrations;
	private final List<BigDecimal> coConcentrations;
	private final List<BigDecimal> noxConcentrations;
	private final List<BigDecimal> soxConcentrations;
	private final LocalTime gasAnalyzerStartTime;   // 가스분석기 측정시작시간
	private final LocalTime thcAnalyzerStartTime;   // THC분석기 측정시작시간
	// ----- 배출가스 계산 -----
	private final BigDecimal standardGasDensity;
	private final BigDecimal oxygenCorrectionFactor;

	// ===== 유량 계산 =====
	private final BigDecimal area;                 // 단면적 (m²)
	private final BigDecimal avgGasTemperatureCelsius; // 평균 온도 avgTs (℃)
	private final BigDecimal avgGasTemperature;    // 평균 절대온도 avgTg (K)
	private final BigDecimal avgDynamicPressure;   // 평균 동압 avgPv
	private final BigDecimal avgStaticPressure;    // 평균 정압 avgPs
	private final BigDecimal gasDensity;           // 현장 배출가스 밀도
	private final BigDecimal pitotCoefficient;     // 피토관 계수 Cp
	private final BigDecimal gasVelocity;          // 평균 유속 Vs (m/s)
	private final BigDecimal quantity;             // 현장 습윤 유량 (m³/h)
	private final BigDecimal standardQuantity;     // 표준상태 건조 유량 (Sm³/h)
	private final BigDecimal gasMeterAbsoluteTemperature; // avgTm (K)

	// ===== 입자상 집계 (nullable) =====
	// 입력
	private final String thimbleFilter;
	private final String bgThimbleFilter;
	private final LocalTime particleSamplingStartTime;
	private final LocalTime particleSamplingEndTime;
	// 계산
	private final BigDecimal avgKFactor;
	private final BigDecimal avgOrificeDp;
	private final BigDecimal avgIsokineticRatio;
	private final BigDecimal totalDryGasVolume;    // totalVm
	private final BigDecimal totalSamplingTime;

	// ===== 목록 (jx:each 대상) =====
	private final List<PointExportView> points;
	private final List<SampleExportView> samples;
}
