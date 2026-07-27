package com.ensolution.ems.schedule.application.command.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 측정 시트의 날씨 값을 노출하는 엑셀 뷰({@code sheet.weather.*}).
 * 대기압은 입력 원값(hPa)과 환산값(mmHg)을 모두 제공하며, 이름의 단위 접미사로 구분한다.
 * jxls(JEXL)는 표준 getter로 프로퍼티를 해석하므로 record가 아니라 {@code @Getter} 클래스로 둔다.
 */
@Getter
@Builder
@AllArgsConstructor
public class WeatherExportView {

	private final BigDecimal pressureHpa;   // 대기압 입력 (hPa)
	private final BigDecimal pressureMmHg;  // 대기압 환산 (mmHg)
	private final String condition;         // 날씨 (맑음/흐림/비/눈)
	private final BigDecimal temperature;   // 외기온도 (℃)
	private final BigDecimal humidity;      // 상대습도 (%)
	private final String windDirection;     // 풍향 (북/북북동 등)
	private final BigDecimal windSpeed;     // 풍속 (m/s)
}
