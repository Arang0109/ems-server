package com.ensolution.ems.schedule.application.calculation.step;

import com.ensolution.ems.schedule.application.calculation.SheetContext;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
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
				: sheet.getMoisture().toBuilder().Xw(context.getXw()).build())
			.exhaustGas(sheet.getExhaustGas() == null ? null
				: sheet.getExhaustGas().toBuilder()
					.gasDensity(context.getStandardGasDensity())
					.o2CorrectionFactor(context.getOxygenCorrectionFactor())
					.build())
			.particleSample(sheet.getParticleSample() == null ? null
				: sheet.getParticleSample().toBuilder().Cp(context.getCp()).build())
			.avgTg(context.getAvgTg())
			.avgPv(context.getAvgPv())
			.avgPs(context.getAvgPs())
			.avgTm(context.getAvgTm())
			.build();

		context.setSheet(updated);
	}
}
