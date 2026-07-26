package com.ensolution.ems.schedule.application.command.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 피토관 계수표의 한 행에 대응하는 엑셀 뷰(적용 유속 구간별 계수).
 * jxls(JEXL)는 표준 getter로 프로퍼티를 해석하므로 record가 아니라 {@code @Getter} 클래스로 둔다.
 */
@Getter
@Builder
@AllArgsConstructor
public class PitotCoefficientExportView {

	private final BigDecimal velocity;     // 적용 유속 하한(m/s)
	private final BigDecimal coefficient;  // 피토관 계수(Cp)
}
