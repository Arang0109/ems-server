package com.ensolution.ems.schedule.application.command.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * 측정 시트의 배출가스 분석 값을 노출하는 엑셀 뷰({@code sheet.gas.*}).
 * 성분 농도는 측정점 순서를 따르는 목록이며, 값이 없어도 null이 아닌 빈 리스트다
 * (템플릿의 jx:each가 null에서 깨지지 않도록).
 * jxls(JEXL)는 표준 getter로 프로퍼티를 해석하므로 record가 아니라 {@code @Getter} 클래스로 둔다.
 */
@Getter
@Builder
@AllArgsConstructor
public class GasExportView {

	// ===== 입력 =====
	private final List<BigDecimal> o2;             // 측정점별 O2 농도 (vol %)
	private final List<BigDecimal> co2;            // 측정점별 CO2 농도 (vol %)
	private final List<BigDecimal> co;             // 측정점별 CO 농도 (vol %)
	private final List<BigDecimal> nox;            // 측정점별 NOx 농도 (ppm)
	private final List<BigDecimal> sox;            // 측정점별 SOx 농도 (ppm)
	private final LocalTime analyzerStartTime;     // 가스분석기 측정 시작시각
	private final LocalTime thcStartTime;          // THC분석기 측정 시작시각

	// ===== 계산 =====
	private final BigDecimal standardDensity;      // 표준상태 습윤 배출가스 밀도 (kg/Sm³)
	private final BigDecimal o2CorrectionFactor;   // 산소보정계수 (무차원)
}
