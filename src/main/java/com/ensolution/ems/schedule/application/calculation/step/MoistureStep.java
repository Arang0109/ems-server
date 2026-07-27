package com.ensolution.ems.schedule.application.calculation.step;

import com.ensolution.ems.schedule.application.calculation.SheetContext;
import com.ensolution.ems.schedule.domain.sheet.MoistureData;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 수분량 Xw(%)를 계산한다. 흡습 수분질량·건조가스량을 STP로 보정해 부피 백분율을 구한다.
 * 가스미터 게이지압의 inchH2O 환산값(Pm_g_inch)도 함께 산출한다.
 */
@Component
@Order(3)
public class MoistureStep implements SheetStep {

	/** mmH2O → inchH2O (1 inch = 25.4 mm). */
	private static final BigDecimal MM_PER_INCH = BigDecimal.valueOf(25.4);

	@Override
	public void execute(SheetContext context) {
		MoistureData moisture = context.getSheet().getMoisture();
		if (moisture == null) return;

		// Xw 계산에 필요한 값이 없어도 게이지압 환산은 독립적으로 제공한다
		setGaugePressureInch(context, moisture);

		MoistureData.BattleWeight weight = moisture.getWeight();
		MoistureData.GasMeterTemperature temp = moisture.getGasMeterTemperature();
		MoistureData.DryGasVolume volume = moisture.getDryGasVolume();
		if (weight == null || weight.getAfter() == null || weight.getBefore() == null) return;
		if (temp == null || temp.getIn() == null || temp.getOut() == null) return;
		if (volume == null || volume.getAfter() == null || volume.getBefore() == null) return;
		if (context.getPa() == null || context.getPm_g() == null) return;

		BigDecimal ma = weight.getAfter().subtract(weight.getBefore());
		BigDecimal Tm_g = temp.getIn().add(temp.getOut()).divide(BigDecimal.TWO, 1, RoundingMode.HALF_UP);
		BigDecimal Vm_g = volume.getAfter().subtract(volume.getBefore());

		BigDecimal Pm = context.getPa().add(context.getPm_g());
		BigDecimal waterVolStp = calcWaterVolumeStp(ma);
		BigDecimal dryVolStp = convertToSTP(Vm_g, Tm_g, Pm);

		BigDecimal denominator = waterVolStp.add(dryVolStp);
		if (denominator.signum() != 0) {
			context.setXw(calcMoistureRatio(waterVolStp, denominator));
		}
		context.setTm_g(Tm_g);
		context.setVm_g(Vm_g);
		context.setMa(ma);
	}

	/** 입력 게이지압(mmH2O)을 inchH2O로 환산한다. 원값에서 바로 나누어 mmHg 환산의 반올림 오차를 타지 않는다. */
	private void setGaugePressureInch(SheetContext context, MoistureData moisture) {
		BigDecimal gaugePressure = moisture.getGasMeterGaugePressure();
		if (gaugePressure == null) return;
		context.setPm_g_inch(gaugePressure.divide(MM_PER_INCH, 3, RoundingMode.HALF_UP));
	}

	private BigDecimal convertToSTP(BigDecimal value, BigDecimal temperature, BigDecimal pressure) {
		BigDecimal t = BigDecimal.valueOf(273);
		BigDecimal p = BigDecimal.valueOf(760);
		return value
			.multiply(t.divide(t.add(temperature), 5, RoundingMode.HALF_UP))
			.multiply(pressure.divide(p, 5, RoundingMode.HALF_UP));
	}

	private BigDecimal calcWaterVolumeStp(BigDecimal waterG) {
		return waterG.multiply(BigDecimal.valueOf(22.4).divide(BigDecimal.valueOf(18), 5, RoundingMode.HALF_UP));
	}

	private BigDecimal calcMoistureRatio(BigDecimal waterVolStp, BigDecimal denominator) {
		return BigDecimal.valueOf(100).multiply(waterVolStp.divide(denominator, 5, RoundingMode.HALF_UP));
	}
}
