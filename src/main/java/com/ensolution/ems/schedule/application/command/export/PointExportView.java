package com.ensolution.ems.schedule.application.command.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 측정점 하나에 대응하는 엑셀 뷰. 입력값과 계산값을 모두 노출한다(고객 양식에 자체 계산식이 있을 수 있으므로 원시 입력도 제공).
 * 유량 입력은 항상, 등속흡인 관련 값은 입자상 측정점일 때만 채워진다.
 * jxls(JEXL)는 표준 getter로 프로퍼티를 해석하므로 record가 아니라 {@code @Getter} 클래스로 둔다.
 */
@Getter
@Builder
@AllArgsConstructor
public class PointExportView {

	private final int index;              // 1부터 시작하는 측정점 번호

	// ===== 유량 입력 =====
	private final BigDecimal gasTemperature;  // Ts 배출가스 온도
	private final BigDecimal dynamicPressure; // Pv 동압
	private final BigDecimal staticPressure;  // Ps 정압

	// ===== 유량 계산 =====
	private final BigDecimal gasVelocity;     // Vs 유속
	private final BigDecimal gasDensity;      // 측정점 배출가스 밀도

	// ===== 입자상 입력 (nullable) =====
	private final BigDecimal nozzleSize;              // 노즐 사이즈
	private final BigDecimal samplingTime;            // 채취 시간
	private final BigDecimal vacuumGaugePressure;     // 진공게이지 압력
	private final BigDecimal finalImpingerTemperature;// 최종 임핀저 온도
	private final BigDecimal inTemperature;           // 가스미터 입구온도 (inTm)
	private final BigDecimal outTemperature;          // 가스미터 출구온도 (outTm)
	private final BigDecimal dryGasVolumeBefore;      // 건식가스미터 채취 전 (beforeVm)
	private final BigDecimal dryGasVolumeAfter;       // 건식가스미터 채취 후 (afterVm)

	// ===== 입자상 계산 (nullable) =====
	private final BigDecimal gasMeterAvgTemperature;  // (inTm+outTm)/2
	private final BigDecimal dryGasVolume;            // Vm 건식가스미터 채취량
	private final BigDecimal collectedWater;          // Vlc 채취된 물의 총량
	private final BigDecimal kFactor;
	private final BigDecimal orificeDp;               // 오리피스 차압
	private final BigDecimal isokineticRatio;         // 등속흡입계수
}
