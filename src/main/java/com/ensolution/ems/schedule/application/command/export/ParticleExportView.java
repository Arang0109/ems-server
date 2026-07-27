package com.ensolution.ems.schedule.application.command.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 측정 시트의 입자상 채취 집계 값을 노출하는 엑셀 뷰({@code sheet.particle.*}).
 * 입자상(먼지/중금속/수은) 시트에서만 값이 채워지며, 가스상 시트에서는 뷰는 존재하되 전 필드가 null이다.
 * jxls(JEXL)는 표준 getter로 프로퍼티를 해석하므로 record가 아니라 {@code @Getter} 클래스로 둔다.
 */
@Getter
@Builder
@AllArgsConstructor
public class ParticleExportView {

	// ===== 입력 =====
	private final String thimbleFilter;            // 원통여지 번호
	private final String blankThimbleFilter;       // 바탕(공시료) 원통여지 번호
	private final LocalTime startTime;             // 입자상 채취 시작시각
	private final LocalTime endTime;               // 입자상 채취 종료시각

	// ===== 계산 =====
	private final BigDecimal avgKFactor;           // 평균 K계수 (무차원)
	private final BigDecimal avgOrificePressure;   // 평균 오리피스 차압 (mmH2O)
	private final BigDecimal avgIsokineticRatio;   // 평균 등속흡입계수 (%)
	private final BigDecimal totalDryGasVolume;    // 총 건식가스미터 채취량 (m³)
	private final BigDecimal totalSamplingTime;    // 총 채취시간 (min)
	private final BigDecimal avgMeterTemperatureK; // 가스미터 평균 절대온도 avgTm (K)
}
