package com.ensolution.ems.schedule.application.command.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 측정 시트의 유량 집계 값을 노출하는 엑셀 뷰({@code sheet.flow.*}). 카테고리와 무관하게 항상 채워진다.
 * 평균 온도는 섭씨({@code avgTemperature})와 절대온도({@code avgTemperatureK})를 모두 제공하며,
 * 유량은 현장 습윤({@code quantity})과 표준상태 건조({@code standardQuantity})를 구분해 제공한다.
 * jxls(JEXL)는 표준 getter로 프로퍼티를 해석하므로 record가 아니라 {@code @Getter} 클래스로 둔다.
 */
@Getter
@Builder
@AllArgsConstructor
public class FlowExportView {

	private final BigDecimal area;                // 측정시설 단면적 (m²)
	private final BigDecimal avgTemperature;      // 평균 배출가스 온도 avgTs (℃)
	private final BigDecimal avgTemperatureK;     // 평균 배출가스 절대온도 avgTg (K)
	private final BigDecimal avgDynamicPressure;  // 평균 동압 avgPv (mmH2O)
	private final BigDecimal avgStaticPressure;   // 평균 정압 avgPs (mmH2O)
	private final BigDecimal density;             // 현장 배출가스 밀도 (kg/m³)
	private final BigDecimal pitotCoefficient;    // 피토관 계수 Cp (무차원)
	private final BigDecimal velocity;            // 평균 유속 Vs (m/s)
	private final BigDecimal quantity;            // 현장 습윤 유량 (m³/h)
	private final BigDecimal standardQuantity;    // 표준상태 건조 유량 (Sm³/h)
}
