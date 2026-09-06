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
	// 입력값
	private final BigDecimal m1;
	private final BigDecimal m2;
	private final BigDecimal t1;
	private final BigDecimal t2;
	private final BigDecimal v1;
	private final BigDecimal v2;
	private final BigDecimal suctionVelocity;
	private final BigDecimal pmMmH2O;
	private final LocalTime startTime;
	private final LocalTime endTime;
	
	// 계산값
	private final BigDecimal xw;        // 수분량 (%)
	private final BigDecimal ma;        // 흡습 수분질량 (g)
	private final BigDecimal tmG;       // 평균 가스미터 온도 (℃)
	private final BigDecimal vmG;       // 건조가스량 (L)
	private final BigDecimal pmG;       // 게이지압 (mmHg)
	private final BigDecimal pmGInch;   // 게이지압 (inchH2O)
	private final Integer samplingTime;
}
