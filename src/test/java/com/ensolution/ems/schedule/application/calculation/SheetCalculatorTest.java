package com.ensolution.ems.schedule.application.calculation;

import com.ensolution.ems.schedule.application.calculation.step.ApplyResultStep;
import com.ensolution.ems.schedule.application.calculation.step.DensityStep;
import com.ensolution.ems.schedule.application.calculation.step.ExhaustGasStep;
import com.ensolution.ems.schedule.application.calculation.step.FlowStep;
import com.ensolution.ems.schedule.application.calculation.step.InitStep;
import com.ensolution.ems.schedule.application.calculation.step.MoistureStep;
import com.ensolution.ems.schedule.application.calculation.step.PressureStep;
import com.ensolution.ems.schedule.application.calculation.step.SheetStep;
import com.ensolution.ems.schedule.domain.sheet.ExhaustGasData;
import com.ensolution.ems.schedule.domain.sheet.MeasurementPoint;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import com.ensolution.ems.schedule.domain.sheet.MoistureData;
import com.ensolution.ems.schedule.domain.sheet.WeatherData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 측정 시트 계산 파이프라인이 실제 값을 산출하는지 검증하는 스모크 테스트. */
class SheetCalculatorTest {

	private SheetCalculator calculator() {
		Calculator calc = new Calculator();
		List<SheetStep> steps = List.of(
			new InitStep(calc),
			new PressureStep(),
			new MoistureStep(),
			new ExhaustGasStep(calc),
			new DensityStep(calc),
			new FlowStep(),
			new ApplyResultStep()
		);
		return new SheetCalculator(steps);
	}

	@Test
	void 대기압과_평균온도_수분량을_계산한다() {
		MeasurementSheet sheet = MeasurementSheet.builder()
			.weather(WeatherData.builder()
				.pressure(WeatherData.Pressure.builder().pressure(new BigDecimal("1013.25")).unit("hPa").build())
				.build())
			.moisture(MoistureData.builder()
				.weight(MoistureData.Weight.builder().before(new BigDecimal("10")).after(new BigDecimal("15")).build())
				.gasMeterTemperature(MoistureData.GasMeterTemperature.builder().in(new BigDecimal("20")).out(new BigDecimal("22")).build())
				.dryGasVolume(MoistureData.DryGasVolume.builder().before(new BigDecimal("0")).after(new BigDecimal("50")).build())
				.gasMeterGaugePressure(new BigDecimal("0"))
				.build())
			.exhaustGas(ExhaustGasData.builder()
				.o2Concentration(List.of(new BigDecimal("10")))
				.co2Concentration(List.of(new BigDecimal("8")))
				.coConcentration(List.of(new BigDecimal("0")))
				.build())
			.measurementPoints(List.of(
				MeasurementPoint.builder().Ts(new BigDecimal("100")).Pv(new BigDecimal("5")).Ps(new BigDecimal("-2")).build(),
				MeasurementPoint.builder().Ts(new BigDecimal("100")).Pv(new BigDecimal("5")).Ps(new BigDecimal("-2")).build()
			))
			.build();

		// 표준산소농도는 측정시설에서 오며, 여기서는 계산 입력으로 직접 전달한다.
		MeasurementSheet result = calculator().calculate(sheet, new BigDecimal("4"), null);

		// 대기압: 1013.25 hPa → 760.0 mmHg
		assertThat(result.getWeather().getPa()).isEqualByComparingTo("760.0");
		// 배출가스 절대온도: (100+273) 평균 = 373.0 K
		assertThat(result.getAvgTg()).isEqualByComparingTo("373.0");
		assertThat(result.getAvgPv()).isEqualByComparingTo("5.0");
		assertThat(result.getAvgPs()).isEqualByComparingTo("-2.0");
		// 수분량 Xw가 산출됨(양수)
		assertThat(result.getMoisture().getXw()).isNotNull();
		assertThat(result.getMoisture().getXw().signum()).isPositive();
		// 산소보정계수: (21-4)/(21-10) = 17/11 ≈ 1.54545
		assertThat(result.getExhaustGas().getO2CorrectionFactor()).isEqualByComparingTo("1.54545");
		// 표준 가스밀도가 산출됨
		assertThat(result.getExhaustGas().getGasDensity()).isNotNull();
	}

	@Test
	void 입력이_비어도_예외없이_통과한다() {
		MeasurementSheet empty = MeasurementSheet.builder()
			.category(null)
			.build();

		MeasurementSheet result = calculator().calculate(empty, null, null);

		assertThat(result).isNotNull();
	}
}
