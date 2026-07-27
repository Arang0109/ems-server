package com.ensolution.ems.schedule.application.command.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 측정점 하나에 대응하는 엑셀 뷰. 입력값과 계산값을 모두 노출한다(고객 양식에 자체 계산식이 있을 수 있으므로 원시 입력도 제공).
 * 유량 입력은 항상, 등속흡인 관련 값은 입자상 측정점일 때만 채워진다.
 * 가스미터 관련 필드는 {@link MoistureExportView}와 같은 어휘를 쓴다(inTemperature/outTemperature/volumeBefore 등).
 * jxls(JEXL)는 표준 getter로 프로퍼티를 해석하므로 record가 아니라 {@code @Getter} 클래스로 둔다.
 */
@Getter
@Builder
@AllArgsConstructor
public class PointExportView {

	private final int index;              // 1부터 시작하는 측정점 번호

	// ===== 유량 입력 =====
	private final BigDecimal temperature;     // 배출가스 온도 Ts (℃)
	private final BigDecimal dynamicPressure; // 동압 Pv (mmH2O)
	private final BigDecimal staticPressure;  // 정압 Ps (mmH2O)

	// ===== 유량 계산 =====
	private final BigDecimal velocity;        // 유속 Vs (m/s)
	private final BigDecimal density;         // 측정점 배출가스 밀도 (kg/m³)

	// ===== 입자상 입력 (nullable) =====
	private final BigDecimal nozzleSize;           // 노즐 직경 (cm)
	private final BigDecimal samplingTime;         // 채취 시간 (min)
	private final BigDecimal vacuumPressure;       // 진공게이지 압력
	private final BigDecimal impingerTemperature;  // 최종 임핀저 온도 (℃)
	private final BigDecimal inTemperature;        // 가스미터 입구온도 (℃)
	private final BigDecimal outTemperature;       // 가스미터 출구온도 (℃)
	private final BigDecimal volumeBefore;         // 건식가스미터 채취 전 적산값 (m³)
	private final BigDecimal volumeAfter;          // 건식가스미터 채취 후 적산값 (m³)

	// ===== 입자상 계산 (nullable) =====
	private final BigDecimal avgTemperature;       // 가스미터 평균온도 (inTm+outTm)/2 (℃)
	private final BigDecimal dryGasVolume;         // 건식가스미터 채취량 Vm (m³)
	private final BigDecimal collectedWater;       // 채취된 물의 총량 Vlc (mL)
	private final BigDecimal kFactor;              // K계수 (무차원)
	private final BigDecimal orificePressure;      // 오리피스 차압 (mmH2O)
	private final BigDecimal isokineticRatio;      // 등속흡입계수 (%)
}
