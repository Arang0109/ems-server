package com.ensolution.ems.schedule.application.command.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 시료 채취 정보 하나에 대응하는 엑셀 뷰(순수 입력 데이터).
 * 가스미터 관련 필드는 {@link MoistureExportView}·{@link PointExportView}와 같은 어휘를 쓴다.
 * jxls(JEXL)는 표준 getter로 프로퍼티를 해석하므로 record가 아니라 {@code @Getter} 클래스로 둔다.
 */
@Getter
@Builder
@AllArgsConstructor
public class SampleExportView {

	private final String name;                  // 시료명
	private final String number;                // 시료 번호
	private final String blankNumber;           // 공시료 번호
	private final LocalTime startTime;          // 채취 시작시각
	private final LocalTime endTime;            // 채취 종료시각
	private final BigDecimal suctionQuantity;   // 흡입량
	private final BigDecimal gaugePressure;     // 가스미터 게이지압 (mmH2O)
	private final BigDecimal inTemperature;     // 가스미터 입구온도 (℃)
	private final BigDecimal outTemperature;    // 가스미터 출구온도 (℃)
	private final BigDecimal volumeBefore;      // 채취 전 부피 (L)
	private final BigDecimal volumeAfter;       // 채취 후 부피 (L)
	private final BigDecimal samplingVolume;    // 채취 부피 (L)
}
