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
import com.ensolution.ems.schedule.application.command.StackData;
import com.ensolution.ems.schedule.domain.sheet.ExhaustGasData;
import com.ensolution.ems.schedule.domain.sheet.MeasurementCategory;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import com.ensolution.ems.schedule.domain.sheet.MoistureData;
import com.ensolution.ems.schedule.domain.sheet.SamplingPoint;
import com.ensolution.ems.schedule.domain.sheet.SamplingPoint.ParticleSampling;
import com.ensolution.ems.schedule.domain.sheet.WeatherData;
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
		return WeatherData.builder().pressure(new BigDecimal("1013.25")).build();
	}

	private MoistureData moisture() {
		return MoistureData.builder()
			.weight(MoistureData.BattleWeight.builder().before(new BigDecimal("10")).after(new BigDecimal("15")).build())
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
	void 가스상_시트_유량까지_계산한다() {
		MeasurementSheet sheet = MeasurementSheet.builder()
			.category(MeasurementCategory.GAS)
			.weather(weather())
			.moisture(moisture())
			.exhaustGas(exhaustGas())
			.samplingPoints(List.of(
				SamplingPoint.builder().Ts(new BigDecimal("100")).Pv(new BigDecimal("5")).Ps(new BigDecimal("-2")).build(),
				SamplingPoint.builder().Ts(new BigDecimal("100")).Pv(new BigDecimal("5")).Ps(new BigDecimal("-2")).build()
			))
			.build();

		MeasurementSheet result = calculator().calculate(sheet, stackData(), pitotCoefficients(), null, DELTA_H);

		// 대기압: 1013.25 hPa → 760.0 mmHg
		assertThat(result.getWeather().getPa()).isEqualByComparingTo("760.0");
		// 수분량 Xw가 산출됨(양수)
		assertThat(result.getMoisture().getXw()).isNotNull();
		assertThat(result.getMoisture().getXw().signum()).isPositive();
		// 산소보정계수: (21-4)/(21-10) = 17/11 ≈ 1.54545
		assertThat(result.getExhaustGas().getO2CorrectionFactor()).isEqualByComparingTo("1.54545");
		// 규정상 요구 측정점 수: 원형 지름 1m ≤ 1 → 1 (클라이언트가 보낸 2점 배열 길이가 아님)
		assertThat(result.getSamplingPointCnt()).isEqualTo(1);

		// 유량 집계
		assertThat(result.getQuantity()).isNotNull();
		// 단면적: 원형 π·1²/4 ≈ 0.785
		assertThat(result.getQuantity().getArea()).isEqualByComparingTo("0.785");
		// 평균값: 절대온도 373.0K, 동압 5.0, 정압 -2.0
		assertThat(result.getQuantity().getAvgTg()).isEqualByComparingTo("373.0");
		assertThat(result.getQuantity().getAvgPv()).isEqualByComparingTo("5.0");
		assertThat(result.getQuantity().getAvgPs()).isEqualByComparingTo("-2.0");
		// 피토관 계수, 유속, 유량이 산출됨(양수)
		assertThat(result.getQuantity().getCp()).isEqualByComparingTo("0.84");
		assertThat(result.getQuantity().getVs()).isNotNull();
		assertThat(result.getQuantity().getVs().signum()).isPositive();
		assertThat(result.getQuantity().getQuantity().signum()).isPositive();
		assertThat(result.getQuantity().getStandardQuantity().signum()).isPositive();
		// 표준상태 건조 유량은 보정계수(<1)를 곱하므로 현장 습윤 유량보다 작다
		assertThat(result.getQuantity().getStandardQuantity())
			.isLessThan(result.getQuantity().getQuantity());

		// 가스상 시트는 입자상 없음
		assertThat(result.getParticle()).isNull();
		assertThat(result.getSamplingPoints().get(0).getParticle()).isNull();
	}

	@Test
	void 입자상_시트_등속흡인계수까지_계산한다() {
		SamplingPoint particlePoint = SamplingPoint.builder()
			.Ts(new BigDecimal("100")).Pv(new BigDecimal("5")).Ps(new BigDecimal("-2"))
			.particle(ParticleSampling.builder()
				.nozzleSize(new BigDecimal("0.6"))
				.samplingTime(new BigDecimal("30"))
				.equipmentTemperature(ParticleSampling.EquipmentTemperature.builder()
					.inTm(new BigDecimal("20")).outTm(new BigDecimal("22")).build())
				.equipmentVolume(ParticleSampling.EquipmentVolume.builder()
					.beforeVm(new BigDecimal("0")).afterVm(new BigDecimal("1.5")).build())
				.build())
			.build();

		MeasurementSheet sheet = MeasurementSheet.builder()
			.category(MeasurementCategory.DUST)
			.weather(weather())
			.moisture(moisture())
			.exhaustGas(exhaustGas())
			.samplingPoints(List.of(particlePoint))
			.build();

		MeasurementSheet result = calculator().calculate(sheet, stackData(), pitotCoefficients(), null, DELTA_H);

		SamplingPoint point = result.getSamplingPoints().get(0);
		ParticleSampling ps = point.getParticle();
		assertThat(ps).isNotNull();

		// 측정점별 등속흡인 결과가 모두 산출됨(양수)
		assertThat(ps.getKFactor()).isNotNull();
		assertThat(ps.getKFactor().signum()).isPositive();
		assertThat(ps.getVlc()).isNotNull();
		assertThat(ps.getVlc().signum()).isPositive();
		assertThat(ps.getIsokineticRatio()).isNotNull();
		assertThat(ps.getIsokineticRatio().signum()).isPositive();
		assertThat(point.getVs()).isNotNull();
		assertThat(point.getVs().signum()).isPositive();

		// Vm = afterVm - beforeVm = 1.5
		assertThat(ps.getVm()).isEqualByComparingTo("1.5");
		// avgTm = (inTm + outTm) / 2 = 21.0
		assertThat(ps.getEquipmentTemperature().getAvgTm()).isEqualByComparingTo("21.0");
		// orificeDp = kFactor × Pv (Pv=5), round 2
		assertThat(ps.getOrificeDp())
			.isEqualByComparingTo(ps.getKFactor().multiply(new BigDecimal("5")).setScale(2, RoundingMode.HALF_UP));

		// 입자상 집계 산출
		assertThat(result.getParticle()).isNotNull();
		assertThat(result.getParticle().getAvgKFactor()).isEqualByComparingTo(ps.getKFactor());
		assertThat(result.getParticle().getAvgIsokineticRatio()).isEqualByComparingTo(ps.getIsokineticRatio());
		assertThat(result.getParticle().getTotalVm()).isEqualByComparingTo("1.5");
		assertThat(result.getParticle().getTotalSamplingTime()).isEqualByComparingTo("30");
		// 가스미터 절대온도: 평균((in+out)/2) + 273 = 21 + 273 = 294.0
		assertThat(result.getAvgTm()).isEqualByComparingTo("294.0");
	}

	@Test
	void 입력이_비어도_예외없이_통과한다() {
		MeasurementSheet empty = MeasurementSheet.builder().build();

		MeasurementSheet result = calculator().calculate(empty, null, null, null, null);

		assertThat(result).isNotNull();
	}
}
