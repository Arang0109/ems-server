package com.ensolution.ems.schedule.application.command.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 측정 시트의 수분 측정 값을 노출하는 엑셀 뷰({@code sheet.moisture.*}).
 * 입력값과 계산값을 모두 제공한다(고객 양식에 자체 계산식이 있을 수 있으므로 원시 입력도 남긴다).
 * 가스미터 게이지압은 입력 원값(mmH2O)과 환산값(mmHg·inchH2O)을 모두 제공하며, 이름의 단위 접미사로 구분한다.
 * jxls(JEXL)는 표준 getter로 프로퍼티를 해석하므로 record가 아니라 {@code @Getter} 클래스로 둔다.
 */
@Getter
@Builder
@AllArgsConstructor
public class MoistureExportView {

	// ===== 입력 =====
	private final BigDecimal weightBefore;        // 흡습병 무게 전 (g)
	private final BigDecimal weightAfter;         // 흡습병 무게 후 (g)
	private final BigDecimal inTemperature;       // 가스미터 입구온도 (℃)
	private final BigDecimal outTemperature;      // 가스미터 출구온도 (℃)
	private final BigDecimal volumeBefore;        // 건조가스 적산 부피 전 (L)
	private final BigDecimal volumeAfter;         // 건조가스 적산 부피 후 (L)
	private final BigDecimal suctionVelocity;     // 흡입속도
	private final BigDecimal gaugePressureMmH2O;  // 가스미터 게이지압 입력 (mmH2O)
	private final LocalTime startTime;            // 수분 채취 시작시각
	private final LocalTime endTime;              // 수분 채취 종료시각

	// ===== 계산 =====
	private final BigDecimal ratio;               // 수분량 Xw (%)
	private final BigDecimal absorbedMass;        // 흡습 수분질량 ma (g)
	private final BigDecimal avgTemperature;      // 가스미터 흡입 가스온도 Tm_g (℃)
	private final BigDecimal dryGasVolume;        // 흡입 건조가스량 Vm_g (L)
	private final BigDecimal gaugePressureMmHg;   // 가스미터 게이지압 Pm_g (mmHg)
	private final BigDecimal gaugePressureInchH2O;// 가스미터 게이지압 Pm_g_inch (inchH2O)
}
