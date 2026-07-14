package com.ensolution.ems.schedule.application.calculation.step;

import com.ensolution.ems.schedule.application.calculation.Calculator;
import com.ensolution.ems.schedule.application.calculation.SheetContext;
import com.ensolution.ems.schedule.domain.sheet.ExhaustGasData;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** 배출가스 평균 농도(O2·CO2·CO·N2), 산소보정계수, 건조·습윤 분자량을 계산한다. */
@Component
@Order(4)
@RequiredArgsConstructor
public class ExhaustGasStep implements SheetStep {

	private final Calculator calculator;

	private static final BigDecimal O2_MOL = BigDecimal.valueOf(0.32);
	private static final BigDecimal CO2_MOL = BigDecimal.valueOf(0.44);
	private static final BigDecimal CO_MOL = BigDecimal.valueOf(0.28);
	private static final BigDecimal N2_MOL = BigDecimal.valueOf(0.28);

	@Override
	public void execute(SheetContext context) {
		ExhaustGasData exhaustGas = context.getSheet().getExhaustGas();
		if (exhaustGas == null) return;
		if (isEmpty(exhaustGas.getO2Concentration())
			&& isEmpty(exhaustGas.getCo2Concentration())
			&& isEmpty(exhaustGas.getCoConcentration())) {
			return;
		}

		BigDecimal o2 = calculator.averageTreatNullAsZero(exhaustGas.getO2Concentration(), 1);
		BigDecimal co2 = calculator.averageTreatNullAsZero(exhaustGas.getCo2Concentration(), 1);
		BigDecimal co = calculator.averageTreatNullAsZero(exhaustGas.getCoConcentration(), 1);
		BigDecimal n2 = BigDecimal.valueOf(100).subtract(o2.add(co2).add(co));

		BigDecimal standardOxygen = context.getStandardOxygen();
		if (standardOxygen != null) {
			context.setOxygenCorrectionFactor(calcOxygenCorrectionFactor(standardOxygen, o2));
		}

		BigDecimal Md = calcDryMolecularWeight(o2, co2, co, n2);
		context.setO2(o2);
		context.setCo2(co2);
		context.setCo(co);
		context.setN2(n2);
		context.setMd(Md);

		if (context.getXw() != null) {
			context.setMw(calcMolecularWeight(Md, context.getXw()));
		}
	}

	private boolean isEmpty(List<BigDecimal> values) {
		return values == null || values.isEmpty();
	}

	private BigDecimal calcDryMolecularWeight(BigDecimal o2, BigDecimal co2, BigDecimal co, BigDecimal n2) {
		return O2_MOL.multiply(o2).add(CO2_MOL.multiply(co2)).add(CO_MOL.multiply(co)).add(N2_MOL.multiply(n2));
	}

	private BigDecimal calcMolecularWeight(BigDecimal Md, BigDecimal Xw) {
		BigDecimal B = Xw.divide(BigDecimal.valueOf(100), 5, RoundingMode.HALF_UP);
		return Md.multiply(BigDecimal.ONE.subtract(B)).add(BigDecimal.valueOf(18).multiply(B));
	}

	private BigDecimal calcOxygenCorrectionFactor(BigDecimal standardOxygen, BigDecimal o2) {
		BigDecimal denominator = BigDecimal.valueOf(21).subtract(o2);
		if (denominator.signum() == 0) return null;
		return BigDecimal.valueOf(21).subtract(standardOxygen)
			.divide(denominator, 5, RoundingMode.HALF_UP);
	}
}
