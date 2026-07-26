package com.ensolution.ems.schedule.application.calculation.step;

import com.ensolution.ems.schedule.application.calculation.Calculator;
import com.ensolution.ems.schedule.application.calculation.SheetContext;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 유량을 계산한다. 측정점별이 아니라 평균값 기준으로 단일 산출한다.
 * 평균 유속 Vs = Cp·√(19.62·avgPv/gasDensity), 현장 습윤 유량 = 3600·area·Vs,
 * 표준상태 건조 유량 = 유량·(273/avgTg)·(Pg/760)·(1 − Xw/100).
 */
@Component
@Order(7)
@RequiredArgsConstructor
public class QuantityStep implements SheetStep {

	private static final BigDecimal G2 = BigDecimal.valueOf(19.62); // 2 × 9.81
	private static final BigDecimal SEC_PER_HOUR = BigDecimal.valueOf(3600);
	private static final BigDecimal STD_TEMP = BigDecimal.valueOf(273);
	private static final BigDecimal STD_PRESSURE = BigDecimal.valueOf(760);
	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private final Calculator calculator;

	@Override
	public void execute(SheetContext context) {
		BigDecimal vs = calcGasVelocity(context.getCp(), context.getAvgPv(), context.getGasDensity());
		if (vs == null) return;
		context.setVs(calculator.round(vs, 3));

		BigDecimal area = context.getArea();
		if (area == null) return;
		BigDecimal quantity = SEC_PER_HOUR.multiply(area).multiply(vs);
		context.setQuantity(calculator.round(quantity, 1));

		BigDecimal avgTg = context.getAvgTg();
		BigDecimal Pg = context.getPg();
		BigDecimal Xw = context.getXw();
		if (avgTg == null || avgTg.signum() == 0 || Pg == null || Xw == null) return;

		BigDecimal standardQuantity = quantity
			.multiply(STD_TEMP.divide(avgTg, 5, RoundingMode.HALF_UP))
			.multiply(Pg.divide(STD_PRESSURE, 5, RoundingMode.HALF_UP))
			.multiply(BigDecimal.ONE.subtract(Xw.divide(HUNDRED, 5, RoundingMode.HALF_UP)));
		context.setStandardQuantity(calculator.round(standardQuantity, 1));
	}

	// Vs = Cp × √(19.62 × Pv / gasDensity)
	private BigDecimal calcGasVelocity(BigDecimal Cp, BigDecimal avgPv, BigDecimal gasDensity) {
		if (Cp == null || avgPv == null || gasDensity == null || gasDensity.signum() <= 0) return null;
		BigDecimal value = G2.multiply(avgPv).divide(gasDensity, 5, RoundingMode.HALF_UP);
		if (value.signum() < 0) return null;
		return Cp.multiply(value.sqrt(new MathContext(10)));
	}
}
