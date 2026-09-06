package com.ensolution.ems.schedule.application.calculation.step;

import com.ensolution.ems.schedule.application.calculation.SheetContext;
import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;
import com.ensolution.ems.schedule.domain.sampling.FlowRateData;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 계산 결과를 시트에 반영한다. 파이프라인의 마지막 단계. */
@Component
@Order(999)
public class ApplyResultStep implements SheetStep {

	@Override
	public void execute(SheetContext context) {
		SamplingSheet sheet = context.getSheet();

		SamplingSheet updated = sheet.toBuilder()
			.weather(sheet.getWeather() == null ? null
				: sheet.getWeather().toBuilder().atmosphericPressureMmHg(context.getPa()).build())
			.moisture(sheet.getMoisture() == null ? null
				: sheet.getMoisture().toBuilder()
					.gasMeterGaugePressureMmHg(context.getPm_g())
					.gasMeterGaugePressureInH2O(context.getPm_g_inch())
					.averageGasMeterTemperature(context.getTm_g())
					.sampledDryGasVolume(context.getVm_g())
					.absorbedMoistureMass(context.getMa())
					.moistureRatio(context.getXw())
				.build())
			.exhaustGas(sheet.getExhaustGas() == null ? null
				: sheet.getExhaustGas().toBuilder()
					.standardGasDensity(context.getStandardGasDensity())
					.o2CorrectionFactor(context.getOxygenCorrectionFactor())
					.build())
			.flowRate((sheet.getFlowRate() == null ? FlowRateData.builder() : sheet.getFlowRate().toBuilder())
				.averageGasTemperatureKelvin(context.getAvgTg())
				.averageDynamicPressure(context.getAvgPv())
				.averageStaticPressure(context.getAvgPs())
				.gasDensity(context.getGasDensity())
				.stackArea(context.getArea())
				.averageGasVelocity(context.getVs())
				.wetGasFlowRate(context.getQuantity())
				.standardDryGasFlowRate(context.getStandardQuantity())
				.appliedPitotCoefficient(context.getCp())
				.build())
			.samplingPointCount(context.getSamplingPointCnt())
			.build();

		context.setSheet(updated);
	}
}
