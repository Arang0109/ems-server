package com.ensolution.ems.schedule.application.command.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 시료 채취 정보 하나에 대응하는 엑셀 뷰(순수 입력 데이터).
 * jxls(JEXL)는 표준 getter로 프로퍼티를 해석하므로 record가 아니라 {@code @Getter} 클래스로 둔다.
 */
@Getter
@Builder
@AllArgsConstructor
public class SampleExportView {

	private final String sampleName;
	private final LocalTime startTime;
	private final LocalTime endTime;
	private final BigDecimal suctionQuantity;         // 흡입량
	private final BigDecimal gasMeterGaugePressure;   // 가스미터 게이지압
	private final BigDecimal inTemperature;           // 가스미터 입구온도
	private final BigDecimal outTemperature;          // 가스미터 출구온도
	private final BigDecimal beforeVolume;            // 채취 전 부피
	private final BigDecimal afterVolume;             // 채취 후 부피
	private final String blankSampleNumber;           // 공시료 번호
	private final String sampleNumber;                // 시료 번호
	private final BigDecimal samplingVolume;          // 채취 부피
}
