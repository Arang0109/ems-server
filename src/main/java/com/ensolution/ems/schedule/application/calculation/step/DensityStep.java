package com.ensolution.ems.schedule.application.calculation.step;

import com.ensolution.ems.schedule.application.calculation.Calculator;
import com.ensolution.ems.schedule.application.calculation.SheetContext;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 표준 가스밀도와 현장조건 가스밀도를 계산한다. */
@Component
@Order(5)
@RequiredArgsConstructor
public class DensityStep implements SheetStep {

	private final Calculator calculator;

	private static final BigDecimal SM = BigDecimal.valueOf(22.4);
	private static final BigDecimal O2_MOL = BigDecimal.valueOf(0.32);
	private static final BigDecimal CO2_MOL = BigDecimal.valueOf(0.44);
	private static final BigDecimal CO_MOL = BigDecimal.valueOf(0.28);
	private static final BigDecimal N2_MOL = BigDecimal.valueOf(0.28);

	@Override
	public void execute(SheetContext context) {
		BigDecimal o2 = context.getO2();
		BigDecimal co2 = context.getCo2();
		BigDecimal co = context.getCo();
		BigDecimal n2 = context.getN2();
		BigDecimal Xw = context.getXw();
		if (o2 == null || co2 == null || co == null || n2 == null || Xw == null) return;

		// 현장조건 환산에도 **반올림된** 표준밀도를 물려 쓴다. 성적서 엑셀이 표에 찍힌 값을
		// 그대로 다음 칸의 입력으로 쓰기 때문이다 — 원시값을 넘기면 엑셀과 밀도·유속이 어긋난다
		// (같은 입력에서 0.922 vs 0.924).
		BigDecimal standardGasDensity = calculator.round(calcStandardGasDensity(o2, co2, co, n2, Xw), 2);
		context.setStandardGasDensity(standardGasDensity);

		BigDecimal avgTg = context.getAvgTg();
		BigDecimal Pg = context.getPg();
		if (avgTg == null || avgTg.signum() == 0 || Pg == null) return;
		context.setGasDensity(calculator.round(convertFromSTP(standardGasDensity, avgTg, Pg), 3));
	}

	private BigDecimal convertFromSTP(BigDecimal value, BigDecimal temperature, BigDecimal pressure) {
		BigDecimal t = BigDecimal.valueOf(273);
		BigDecimal p = BigDecimal.valueOf(760);
		return value
			.multiply(t.divide(temperature, 5, RoundingMode.HALF_UP))
			.multiply(pressure.divide(p, 5, RoundingMode.HALF_UP));
	}

	private BigDecimal calcStandardGasDensity(
		BigDecimal o2Avg, BigDecimal co2Avg, BigDecimal coAvg, BigDecimal n2Avg, BigDecimal Xw) {
		BigDecimal o2 = o2Avg.divide(SM, 5, RoundingMode.HALF_UP).multiply(O2_MOL);
		BigDecimal co2 = co2Avg.divide(SM, 5, RoundingMode.HALF_UP).multiply(CO2_MOL);
		BigDecimal co = coAvg.divide(SM, 5, RoundingMode.HALF_UP).multiply(CO_MOL);
		BigDecimal n2 = n2Avg.divide(SM, 5, RoundingMode.HALF_UP).multiply(N2_MOL);

		BigDecimal dryGasDensity = o2.add(co2).add(co).add(n2);
		BigDecimal moisture = BigDecimal.valueOf(18)
			.multiply(Xw)
			.divide(SM, 5, RoundingMode.HALF_UP)
			.divide(BigDecimal.valueOf(100), 5, RoundingMode.HALF_UP);

		return dryGasDensity
			.multiply(BigDecimal.valueOf(100).subtract(Xw))
			.divide(BigDecimal.valueOf(100), 5, RoundingMode.HALF_UP)
			.add(moisture);
	}
}
