package com.ensolution.ems.schedule.application.calculation.step;

import com.ensolution.ems.equipment.domain.spec.PitotTubeSpec;
import com.ensolution.ems.schedule.application.calculation.SheetContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/** 임시 유속을 산출해 피토관 계수 Cp를 조회한다. */
@Component
@Order(6)
public class FlowStep implements SheetStep {

	private static final BigDecimal DEFAULT_CP = BigDecimal.valueOf(0.84);

	@Override
	public void execute(SheetContext context) {
		List<PitotTubeSpec.PitotCoefficient> coefficients = context.getPitotCoefficients();
		if (coefficients == null || coefficients.isEmpty()) return;

		BigDecimal avgPv = context.getAvgPv();
		BigDecimal gasDensity = context.getGasDensity();
		if (avgPv == null || gasDensity == null || gasDensity.signum() <= 0) return;

		BigDecimal value = BigDecimal.valueOf(19.62).multiply(avgPv).divide(gasDensity, 5, RoundingMode.HALF_UP);
		if (value.signum() < 0) return;

		BigDecimal postGasVelocity = DEFAULT_CP.multiply(value.sqrt(new MathContext(10)));
		context.setCp(findPitotTubeCoefficient(coefficients, postGasVelocity));
	}

	private BigDecimal findPitotTubeCoefficient(List<PitotTubeSpec.PitotCoefficient> coefficients, BigDecimal velocity) {
		BigDecimal result = DEFAULT_CP;
		for (PitotTubeSpec.PitotCoefficient pc : coefficients) {
			if (pc.velocity() != null && velocity.compareTo(pc.velocity()) >= 0) {
				result = pc.coefficient();
			}
		}
		return result;
	}
}
