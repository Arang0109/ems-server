package com.ensolution.ems.schedule.application.calculation.step;

import com.ensolution.ems.schedule.application.calculation.SheetContext;
import com.ensolution.ems.schedule.domain.sheet.MoistureData;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 수분량 Xw(%)를 계산한다. 흡습 수분질량·건조가스량을 STP로 보정해 부피 백분율을 구한다. */
@Component
@Order(3)
public class MoistureStep implements SheetStep {

	@Override
	public void execute(SheetContext context) {
		MoistureData moisture = context.getSheet().getMoisture();
		if (moisture == null) return;

		MoistureData.Weight weight = moisture.getWeight();
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
