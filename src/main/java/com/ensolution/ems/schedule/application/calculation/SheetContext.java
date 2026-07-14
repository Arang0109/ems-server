package com.ensolution.ems.schedule.application.calculation;

import com.ensolution.ems.equipment.domain.spec.PitotTubeSpec;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 측정 시트 계산 컨텍스트. 각 스텝이 순서대로 중간값을 축적하고, 마지막에 {@code ApplyResultStep}이
 * 결과를 시트에 반영한다. 계산에 필요한 외부 입력(표준산소농도·피토관 계수)은 생성 시 주입한다.
 */
@Getter
@Setter
public class SheetContext {

	private MeasurementSheet sheet;
	private final BigDecimal standardOxygen;
	private final List<PitotTubeSpec.PitotCoefficient> pitotCoefficients;

	// 압력
	private BigDecimal Pa;    // 대기압 (mmHg)
	private BigDecimal Pg;    // 배출가스 절대압력 (mmHg)
	private BigDecimal Pm_g;  // 수분측정용 가스미터 게이지압 (mmHg)

	// 수분
	private BigDecimal Xw;    // 수분량 (%)
	private BigDecimal Tm_g;  // 가스미터 흡입 가스온도 (°C)
	private BigDecimal Vm_g;  // 흡입 건조가스량 (L)
	private BigDecimal ma;    // 흡습 수분질량 (g)

	// 배출가스
	private BigDecimal o2;
	private BigDecimal co2;
	private BigDecimal co;
	private BigDecimal n2;
	private BigDecimal oxygenCorrectionFactor;
	private BigDecimal Md;    // 건조배출가스 분자량
	private BigDecimal Mw;    // 습윤배출가스 분자량

	// 밀도
	private BigDecimal standardGasDensity;
	private BigDecimal gasDensity;

	// 유속
	private BigDecimal Cp;

	// 평균
	private BigDecimal avgTg; // 배출가스 절대온도 (K)
	private BigDecimal avgPv; // 배출가스 평균 동압
	private BigDecimal avgPs; // 배출가스 평균 정압
	private BigDecimal avgTm; // 가스미터 절대온도 (K)

	public SheetContext(MeasurementSheet sheet, BigDecimal standardOxygen, List<PitotTubeSpec.PitotCoefficient> pitotCoefficients) {
		this.sheet = sheet;
		this.standardOxygen = standardOxygen;
		this.pitotCoefficients = pitotCoefficients;
	}
}
