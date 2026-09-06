package com.ensolution.ems.schedule.application.calculation;

import com.ensolution.ems.equipment.domain.spec.PitotTubeSpec;
import com.ensolution.ems.global.common.enums.Shape;
import com.ensolution.ems.schedule.application.calculation.step.ApplyResultStep;
import com.ensolution.ems.schedule.application.calculation.step.DensityStep;
import com.ensolution.ems.schedule.application.calculation.step.ExhaustGasStep;
import com.ensolution.ems.schedule.application.calculation.step.FlowStep;
import com.ensolution.ems.schedule.application.calculation.step.InitStep;
import com.ensolution.ems.schedule.application.calculation.step.MoistureStep;
import com.ensolution.ems.schedule.application.calculation.step.ParticleStep;
import com.ensolution.ems.schedule.application.calculation.step.PressureStep;
import com.ensolution.ems.schedule.application.calculation.step.QuantityStep;
import com.ensolution.ems.schedule.application.calculation.step.SheetStep;
import com.ensolution.ems.schedule.application.calculation.StackData;
import com.ensolution.ems.schedule.domain.sampling.ExhaustGasData;
import com.ensolution.ems.schedule.domain.sampling.MeasurementCategory;
import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;
import com.ensolution.ems.schedule.domain.sampling.MoistureData;
import com.ensolution.ems.schedule.domain.sampling.SamplingPoint;
import com.ensolution.ems.schedule.domain.sampling.SamplingPoint.IsokineticSamplingData;
import com.ensolution.ems.schedule.domain.sampling.WeatherData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 측정 시트 계산 파이프라인이 실제 값을 산출하는지 검증하는 스모크 테스트. */
class SheetCalculatorTest {

	private static final BigDecimal DELTA_H = new BigDecimal("46");

	private SheetCalculator calculator() {
		Calculator calc = new Calculator();
		List<SheetStep> steps = List.of(
			new InitStep(calc),
			new PressureStep(),
			new MoistureStep(),
			new ExhaustGasStep(calc),
			new DensityStep(calc),
			new FlowStep(),
			new QuantityStep(calc),
			new ParticleStep(calc),
			new ApplyResultStep()
		);
		return new SheetCalculator(steps);
	}

	// Cp 조회를 결정적으로: velocity 0 이상이면 항상 0.84
	private List<PitotTubeSpec.PitotCoefficient> pitotCoefficients() {
		return List.of(new PitotTubeSpec.PitotCoefficient(new BigDecimal("0.84"), BigDecimal.ZERO));
	}

	// 원형 굴뚝, 지름 1m, 표준산소 4%
	private StackData stackData() {
		return new StackData(Shape.CIRCULAR, new BigDecimal("10"),
			new BigDecimal("1"), null, null, new BigDecimal("4"));
	}

	private WeatherData weather() {
		return WeatherData.builder().atmosphericPressure(new BigDecimal("1013.25")).build();
	}

	private MoistureData moisture() {
		return MoistureData.builder()
			.bottleWeight(MoistureData.BottleWeight.builder().before(new BigDecimal("10")).after(new BigDecimal("15")).build())
			.gasMeterTemperature(MoistureData.GasMeterTemperature.builder().in(new BigDecimal("20")).out(new BigDecimal("22")).build())
			.dryGasVolume(MoistureData.DryGasVolume.builder().before(new BigDecimal("0")).after(new BigDecimal("50")).build())
			.gasMeterGaugePressure(new BigDecimal("0"))
			.build();
	}

	private ExhaustGasData exhaustGas() {
		return ExhaustGasData.builder()
			.o2Concentration(List.of(new BigDecimal("10")))
			.co2Concentration(List.of(new BigDecimal("8")))
			.coConcentration(List.of(new BigDecimal("0")))
			.build();
	}

	@Test
	void 가스미터_게이지압을_mmHg와_inchH2O로_함께_환산한다() {
		SamplingSheet sheet = SamplingSheet.builder()
			.category(MeasurementCategory.GAS)
			.weather(weather())
			.moisture(moisture().toBuilder().gasMeterGaugePressure(new BigDecimal("13.6")).build())
			.exhaustGas(exhaustGas())
			.samplingPoints(List.of(
				SamplingPoint.builder().gasTemperature(new BigDecimal("100")).dynamicPressure(new BigDecimal("5")).staticPressure(new BigDecimal("-2")).build()))
			.build();

		MoistureData result = calculator()
			.calculate(sheet, stackData(), pitotCoefficients(), null, DELTA_H)
			.getMoisture();

		// 13.6 mmH2O ÷ 13.6 = 1.00 mmHg
		assertThat(result.getGasMeterGaugePressureMmHg()).isEqualByComparingTo("1.00");
		// 13.6 mmH2O ÷ 25.4 = 0.535 inchH2O
		assertThat(result.getGasMeterGaugePressureInH2O()).isEqualByComparingTo("0.535");
	}

	@Test
	void 가스미터_게이지압이_없으면_inchH2O도_비어있다() {
		SamplingSheet sheet = SamplingSheet.builder()
			.category(MeasurementCategory.GAS)
			.weather(weather())
			.moisture(MoistureData.builder().build())   // 게이지압 미입력
			.exhaustGas(exhaustGas())
			.samplingPoints(List.of(
				SamplingPoint.builder().gasTemperature(new BigDecimal("100")).dynamicPressure(new BigDecimal("5")).staticPressure(new BigDecimal("-2")).build()))
			.build();

		MoistureData result = calculator()
			.calculate(sheet, stackData(), pitotCoefficients(), null, DELTA_H)
			.getMoisture();

		assertThat(result.getGasMeterGaugePressureMmHg()).isNull();
		assertThat(result.getGasMeterGaugePressureInH2O()).isNull();
	}

	@Test
	void 가스상_시트_유량까지_계산한다() {
		SamplingSheet sheet = SamplingSheet.builder()
			.category(MeasurementCategory.GAS)
			.weather(weather())
			.moisture(moisture())
			.exhaustGas(exhaustGas())
			.samplingPoints(List.of(
				SamplingPoint.builder().gasTemperature(new BigDecimal("100")).dynamicPressure(new BigDecimal("5")).staticPressure(new BigDecimal("-2")).build(),
				SamplingPoint.builder().gasTemperature(new BigDecimal("100")).dynamicPressure(new BigDecimal("5")).staticPressure(new BigDecimal("-2")).build()
			))
			.build();

		SamplingSheet result = calculator().calculate(sheet, stackData(), pitotCoefficients(), null, DELTA_H);

		// 대기압: 1013.25 hPa → 760.0 mmHg
		assertThat(result.getWeather().getAtmosphericPressureMmHg()).isEqualByComparingTo("760.0");
		// 수분량 Xw가 산출됨(양수)
		assertThat(result.getMoisture().getMoistureRatio()).isNotNull();
		assertThat(result.getMoisture().getMoistureRatio().signum()).isPositive();
		// 산소보정계수: (21-4)/(21-10) = 17/11 ≈ 1.54545
		assertThat(result.getExhaustGas().getO2CorrectionFactor()).isEqualByComparingTo("1.54545");
		// 규정상 요구 측정점 수: 원형 지름 1m ≤ 1 → 1 (클라이언트가 보낸 2점 배열 길이가 아님)
		assertThat(result.getSamplingPointCount()).isEqualTo(1);

		// 유량 집계
		assertThat(result.getFlowRate()).isNotNull();
		// 단면적: 원형 π·1²/4 ≈ 0.785
		assertThat(result.getFlowRate().getStackArea()).isEqualByComparingTo("0.785");
		// 평균값: 절대온도 373.0K, 동압 5.0, 정압 -2.0
		assertThat(result.getFlowRate().getAverageGasTemperatureKelvin()).isEqualByComparingTo("373.0");
		assertThat(result.getFlowRate().getAverageDynamicPressure()).isEqualByComparingTo("5.0");
		assertThat(result.getFlowRate().getAverageStaticPressure()).isEqualByComparingTo("-2.0");
		// 피토관 계수, 유속, 유량이 산출됨(양수)
		assertThat(result.getFlowRate().getAppliedPitotCoefficient()).isEqualByComparingTo("0.84");
		assertThat(result.getFlowRate().getAverageGasVelocity()).isNotNull();
		assertThat(result.getFlowRate().getAverageGasVelocity().signum()).isPositive();
		assertThat(result.getFlowRate().getWetGasFlowRate().signum()).isPositive();
		assertThat(result.getFlowRate().getStandardDryGasFlowRate().signum()).isPositive();
		// 표준상태 건조 유량은 보정계수(<1)를 곱하므로 현장 습윤 유량보다 작다
		assertThat(result.getFlowRate().getStandardDryGasFlowRate())
			.isLessThan(result.getFlowRate().getWetGasFlowRate());

		// 가스상 시트는 입자상 없음
		assertThat(result.getParticulateSampling()).isNull();
		assertThat(result.getSamplingPoints().get(0).getIsokineticSampling()).isNull();
	}

	@Test
	void 입자상_시트_등속흡인계수까지_계산한다() {
		SamplingPoint particlePoint = SamplingPoint.builder()
			.gasTemperature(new BigDecimal("100")).dynamicPressure(new BigDecimal("5")).staticPressure(new BigDecimal("-2"))
			.isokineticSampling(IsokineticSamplingData.builder()
				.nozzleDiameter(new BigDecimal("0.6"))
				.samplingTime(new BigDecimal("30"))
				.gasTemperature(IsokineticSamplingData.GasMeterTemperature.builder()
					.inlet(new BigDecimal("20")).outlet(new BigDecimal("22")).build())
				.gasMeterVolume(IsokineticSamplingData.GasMeterVolume.builder()
					.before(new BigDecimal("0")).after(new BigDecimal("1.5")).build())
				.build())
			.build();

		SamplingSheet sheet = SamplingSheet.builder()
			.category(MeasurementCategory.DUST)
			.weather(weather())
			.moisture(moisture())
			.exhaustGas(exhaustGas())
			.samplingPoints(List.of(particlePoint))
			.build();

		SamplingSheet result = calculator().calculate(sheet, stackData(), pitotCoefficients(), null, DELTA_H);

		SamplingPoint point = result.getSamplingPoints().get(0);
		IsokineticSamplingData ps = point.getIsokineticSampling();
		assertThat(ps).isNotNull();

		// 측정점별 등속흡인 결과가 모두 산출됨(양수)
		assertThat(ps.getKFactor()).isNotNull();
		assertThat(ps.getKFactor().signum()).isPositive();
		assertThat(ps.getCollectedWaterVolume()).isNotNull();
		assertThat(ps.getCollectedWaterVolume().signum()).isPositive();
		assertThat(ps.getIsokineticRatio()).isNotNull();
		assertThat(ps.getIsokineticRatio().signum()).isPositive();
		assertThat(point.getGasVelocity()).isNotNull();
		assertThat(point.getGasVelocity().signum()).isPositive();

		// 건조가스량 = after - before = 1.5
		assertThat(ps.getSampledDryGasVolume()).isEqualByComparingTo("1.5");
		// 가스미터 평균온도 = (inlet + outlet) / 2 = 21.0
		assertThat(ps.getGasTemperature().getAverage()).isEqualByComparingTo("21.0");
		// 오리피스 차압 = kFactor × 동압(5), round 2
		assertThat(ps.getOrificeDifferentialPressure())
			.isEqualByComparingTo(ps.getKFactor().multiply(new BigDecimal("5")).setScale(2, RoundingMode.HALF_UP));

		// 입자상 집계 산출
		assertThat(result.getParticulateSampling()).isNotNull();
		assertThat(result.getParticulateSampling().getAverageKFactor()).isEqualByComparingTo(ps.getKFactor());
		assertThat(result.getParticulateSampling().getAverageIsokineticRatio()).isEqualByComparingTo(ps.getIsokineticRatio());
		assertThat(result.getParticulateSampling().getTotalDryGasVolume()).isEqualByComparingTo("1.5");
		assertThat(result.getParticulateSampling().getTotalSamplingTime()).isEqualByComparingTo("30");
		// 가스미터 절대온도: 평균((in+out)/2) + 273 = 21 + 273 = 294.0
		assertThat(result.getParticulateSampling().getAverageGasMeterTemperature()).isEqualByComparingTo("294.0");
	}

	@Test
	void 입력이_비어도_예외없이_통과한다() {
		SamplingSheet empty = SamplingSheet.builder().build();

		SamplingSheet result = calculator().calculate(empty, null, null, null, null);

		assertThat(result).isNotNull();
	}
}
