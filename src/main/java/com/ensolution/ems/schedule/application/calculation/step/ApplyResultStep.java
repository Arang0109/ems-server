package com.ensolution.ems.schedule.application.calculation.step;

import com.ensolution.ems.schedule.application.calculation.SheetContext;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import com.ensolution.ems.schedule.domain.sheet.QuantityData;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 계산 결과를 시트에 반영한다. 파이프라인의 마지막 단계. */
@Component
@Order(999)
public class ApplyResultStep implements SheetStep {

	@Override
	public void execute(SheetContext context) {
		MeasurementSheet sheet = context.getSheet();

		MeasurementSheet updated = sheet.toBuilder()
			.weather(sheet.getWeather() == null ? null
				: sheet.getWeather().toBuilder().Pa(context.getPa()).build())
			.moisture(sheet.getMoisture() == null ? null
				: sheet.getMoisture().toBuilder()
					.Pm_g(context.getPm_g())
					.Pm_g_inch(context.getPm_g_inch())
					.Tm_g(context.getTm_g())
					.Vm_g(context.getVm_g())
					.ma(context.getMa())
					.Xw(context.getXw())
				.build())
			.exhaustGas(sheet.getExhaustGas() == null ? null
				: sheet.getExhaustGas().toBuilder()
					.standardGasDensity(context.getStandardGasDensity())
					.o2CorrectionFactor(context.getOxygenCorrectionFactor())
					.build())
			.quantity((sheet.getQuantity() == null ? QuantityData.builder() : sheet.getQuantity().toBuilder())
				.avgTg(context.getAvgTg())
				.avgPv(context.getAvgPv())
				.avgPs(context.getAvgPs())
				.gasDensity(context.getGasDensity())
				.area(context.getArea())
				.Vs(context.getVs())
				.quantity(context.getQuantity())
				.standardQuantity(context.getStandardQuantity())
				.Cp(context.getCp())
				.build())
			.samplingPointCnt(context.getSamplingPointCnt())
			.avgTm(context.getAvgTm())
			.build();

		context.setSheet(updated);
	}
}
