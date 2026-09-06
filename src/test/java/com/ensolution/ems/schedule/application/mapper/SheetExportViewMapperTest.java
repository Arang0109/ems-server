package com.ensolution.ems.schedule.application.mapper;

import com.ensolution.ems.schedule.application.command.export.PointExportView;
import com.ensolution.ems.schedule.application.command.export.SampleExportView;
import com.ensolution.ems.schedule.application.command.export.SheetExportView;
import com.ensolution.ems.schedule.domain.sampling.ExhaustGasData;
import com.ensolution.ems.schedule.domain.sampling.MeasurementCategory;
import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;
import com.ensolution.ems.schedule.domain.sampling.MoistureData;
import com.ensolution.ems.schedule.domain.sampling.ParticulateSampling;
import com.ensolution.ems.schedule.domain.sampling.FlowRateData;
import com.ensolution.ems.schedule.domain.sampling.GaseousSampling;
import com.ensolution.ems.schedule.domain.sampling.SamplingPoint;
import com.ensolution.ems.schedule.domain.sampling.WeatherCondition;
import com.ensolution.ems.schedule.domain.sampling.WeatherData;
import com.ensolution.ems.schedule.domain.sampling.WindDirection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** 측정 시트가 측정 영역별 하위 뷰로 묶여 엑셀 템플릿 계약에 노출되는지 검증한다. */
class SheetExportViewMapperTest {

	private final SheetExportViewMapper mapper = new SheetExportViewMapper();

	@Test
	void 날씨는_입력_hPa와_환산_mmHg를_모두_노출한다() {
		SamplingSheet sheet = SamplingSheet.builder()
			.weather(WeatherData.builder()
				.atmosphericPressure(new BigDecimal("1013.25"))
				.atmosphericPressureMmHg(new BigDecimal("760.0"))
				.weatherCondition(WeatherCondition.CLEAR)
				.temperature(new BigDecimal("21.5"))
				.humidity(new BigDecimal("55"))
				.windDirection(WindDirection.NNE)
				.windSpeed(new BigDecimal("1.2"))
				.build())
			.build();

		SheetExportView view = mapper.toSheetView(sheet);

		assertThat(view.getWeather().getPressureHpa()).isEqualByComparingTo("1013.25");
		assertThat(view.getWeather().getPressureMmHg()).isEqualByComparingTo("760.0");
		assertThat(view.getWeather().getCondition()).isEqualTo("맑음");
		assertThat(view.getWeather().getWindDirection()).isEqualTo("북북동");
		assertThat(view.getWeather().getTemperature()).isEqualByComparingTo("21.5");
	}

	@Test
	void 수분은_입력과_계산값이_그룹_안에서_접두어_없이_노출된다() {
		SamplingSheet sheet = SamplingSheet.builder()
			.moisture(MoistureData.builder()
				.bottleWeight(MoistureData.BottleWeight.builder()
					.before(new BigDecimal("100.0")).after(new BigDecimal("105.4")).build())
				.gasMeterTemperature(MoistureData.GasMeterTemperature.builder()
					.in(new BigDecimal("20")).out(new BigDecimal("22")).build())
				.dryGasVolume(MoistureData.DryGasVolume.builder()
					.before(new BigDecimal("10.0")).after(new BigDecimal("40.0")).build())
				.suctionVelocity(new BigDecimal("1.0"))
				.gasMeterGaugePressure(new BigDecimal("13.6"))
				.samplingStartTime(LocalTime.of(9, 10))
				.samplingEndTime(LocalTime.of(9, 40))
				.moistureRatio(new BigDecimal("11.8"))
				.absorbedMoistureMass(new BigDecimal("5.4"))
				.averageGasMeterTemperature(new BigDecimal("21.0"))
				.sampledDryGasVolume(new BigDecimal("30.0"))
				.gasMeterGaugePressureMmHg(new BigDecimal("1.0"))
				.gasMeterGaugePressureInH2O(new BigDecimal("0.535"))
				.build())
			.build();

		SheetExportView view = mapper.toSheetView(sheet);

		assertThat(view.getMoisture().getM1()).isEqualByComparingTo("100.0");
		assertThat(view.getMoisture().getM2()).isEqualByComparingTo("105.4");
		assertThat(view.getMoisture().getT1()).isEqualByComparingTo("20");
		assertThat(view.getMoisture().getT2()).isEqualByComparingTo("22");
		assertThat(view.getMoisture().getV1()).isEqualByComparingTo("10.0");
		assertThat(view.getMoisture().getV2()).isEqualByComparingTo("40.0");
		assertThat(view.getMoisture().getStartTime()).isEqualTo(LocalTime.of(9, 10));
		assertThat(view.getMoisture().getEndTime()).isEqualTo(LocalTime.of(9, 40));
		// 계산값
		assertThat(view.getMoisture().getXw()).isEqualByComparingTo("11.8");
		assertThat(view.getMoisture().getMa()).isEqualByComparingTo("5.4");
		assertThat(view.getMoisture().getTmG()).isEqualByComparingTo("21.0");
		assertThat(view.getMoisture().getVmG()).isEqualByComparingTo("30.0");
		// 게이지압은 단위 접미사로 입력/환산을 구분한다
		assertThat(view.getMoisture().getPmMmH2O()).isEqualByComparingTo("13.6");
		assertThat(view.getMoisture().getPmG()).isEqualByComparingTo("1.0");
		assertThat(view.getMoisture().getPmGInch()).isEqualByComparingTo("0.535");
	}

	@Test
	void 배출가스_농도는_성분명_그대로_노출된다() {
		SamplingSheet sheet = SamplingSheet.builder()
			.exhaustGas(ExhaustGasData.builder()
				.o2Concentration(List.of(new BigDecimal("10.0"), new BigDecimal("10.4")))
				.co2Concentration(List.of(new BigDecimal("8.0")))
				.gasAnalyzerStartTime(LocalTime.of(9, 0))
				.thcAnalyzerStartTime(LocalTime.of(9, 30))
				.standardGasDensity(new BigDecimal("1.26"))
				.o2CorrectionFactor(new BigDecimal("1.54545"))
				.build())
			.build();

		SheetExportView view = mapper.toSheetView(sheet);

		assertThat(view.getGas().getO2()).containsExactly(new BigDecimal("10.0"), new BigDecimal("10.4"));
		assertThat(view.getGas().getCo2()).containsExactly(new BigDecimal("8.0"));
		// 값이 없는 성분도 jx:each가 깨지지 않도록 빈 리스트
		assertThat(view.getGas().getCo()).isEmpty();
		assertThat(view.getGas().getNox()).isEmpty();
		assertThat(view.getGas().getSox()).isEmpty();
		assertThat(view.getGas().getAnalyzerStartTime()).isEqualTo(LocalTime.of(9, 0));
		assertThat(view.getGas().getThcStartTime()).isEqualTo(LocalTime.of(9, 30));
		assertThat(view.getGas().getStandardDensity()).isEqualByComparingTo("1.26");
		assertThat(view.getGas().getO2CorrectionFactor()).isEqualByComparingTo("1.54545");
	}

	@Test
	void 유량은_섭씨와_절대온도를_이름으로_구분해_노출한다() {
		SamplingSheet sheet = SamplingSheet.builder()
			.flowRate(FlowRateData.builder()
				.stackArea(new BigDecimal("0.785"))
				.averageGasTemperature(new BigDecimal("100.0"))
				.averageGasTemperatureKelvin(new BigDecimal("373.0"))
				.averageDynamicPressure(new BigDecimal("5.0"))
				.averageStaticPressure(new BigDecimal("-2.0"))
				.gasDensity(new BigDecimal("0.95"))
				.appliedPitotCoefficient(new BigDecimal("0.84"))
				.averageGasVelocity(new BigDecimal("12.3"))
				.wetGasFlowRate(new BigDecimal("1000.0"))
				.standardDryGasFlowRate(new BigDecimal("850.0"))
				.build())
			.build();

		SheetExportView view = mapper.toSheetView(sheet);

		assertThat(view.getFlow().getStackArea()).isEqualByComparingTo("0.785");
		assertThat(view.getFlow().getAvgTs()).isEqualByComparingTo("100.0");
		assertThat(view.getFlow().getAvgTg()).isEqualByComparingTo("373.0");
		assertThat(view.getFlow().getAvgPv()).isEqualByComparingTo("5.0");
		assertThat(view.getFlow().getAvgPs()).isEqualByComparingTo("-2.0");
		assertThat(view.getFlow().getGasDensity()).isEqualByComparingTo("0.95");
		assertThat(view.getFlow().getCp()).isEqualByComparingTo("0.84");
		assertThat(view.getFlow().getAvgVs()).isEqualByComparingTo("12.3");
		assertThat(view.getFlow().getQuantity()).isEqualByComparingTo("1000.0");
		assertThat(view.getFlow().getStandardQuantity()).isEqualByComparingTo("850.0");
	}

	@Test
	void 입자상_집계는_가스미터_절대온도까지_한_그룹에_모인다() {
		SamplingSheet sheet = SamplingSheet.builder()
			.category(MeasurementCategory.DUST)
			.particulateSampling(ParticulateSampling.builder()
				.thimbleFilter("TF-001")
				.bgThimbleFilter("TF-B-001")
				.samplingStartedAt(LocalTime.of(10, 0))
				.samplingEndedAt(LocalTime.of(11, 30))
				.averageKFactor(new BigDecimal("0.55"))
				.averageOrificeDifferentialPressure(new BigDecimal("46.3"))
				.averageIsokineticRatio(new BigDecimal("98.7"))
				.totalDryGasVolume(new BigDecimal("1.234"))
				.totalSamplingTime(new BigDecimal("90"))
				.averageGasMeterTemperature(new BigDecimal("294.0"))
				.build())
			.build();

		SheetExportView view = mapper.toSheetView(sheet);

		assertThat(view.getCategory()).isEqualTo("먼지");
		assertThat(view.getParticle().getThimbleFilter()).isEqualTo("TF-001");
		assertThat(view.getParticle().getBlankThimbleFilter()).isEqualTo("TF-B-001");
		assertThat(view.getParticle().getSamplingStartedAt()).isEqualTo(LocalTime.of(10, 0));
		assertThat(view.getParticle().getSamplingEndedAt()).isEqualTo(LocalTime.of(11, 30));
		assertThat(view.getParticle().getAvgOrificePressure()).isEqualByComparingTo("46.3");
		assertThat(view.getParticle().getTotalVm()).isEqualByComparingTo("1.234");
		assertThat(view.getParticle().getTotalSamplingTime()).isEqualByComparingTo("90");
		assertThat(view.getParticle().getAvgTmKelvin()).isEqualByComparingTo("294.0");
	}

	@Test
	void 입자상_집계가_없으면_빈_뷰를_내려_템플릿이_null을_만나지_않게_한다() {
		SheetExportView view = mapper.toSheetView(SamplingSheet.builder().build());

		assertThat(view.getParticle()).isNotNull();
		assertThat(view.getParticle().getAvgTmKelvin()).isNull();
		assertThat(view.getParticle().getThimbleFilter()).isNull();
	}

	@Test
	void 측정점은_1부터_번호가_매겨지고_입자상_값까지_평탄화된다() {
		SamplingPoint point = SamplingPoint.builder()
			.gasTemperature(new BigDecimal("100")).dynamicPressure(new BigDecimal("5")).staticPressure(new BigDecimal("-2"))
			.gasVelocity(new BigDecimal("12.3")).gasDensity(new BigDecimal("0.95"))
			.isokineticSampling(SamplingPoint.IsokineticSamplingData.builder()
				.gasTemperature(SamplingPoint.IsokineticSamplingData.GasMeterTemperature.builder()
					.inlet(new BigDecimal("20")).outlet(new BigDecimal("22")).average(new BigDecimal("21")).build())
				.gasMeterVolume(SamplingPoint.IsokineticSamplingData.GasMeterVolume.builder()
					.before(new BigDecimal("1.0")).after(new BigDecimal("2.2")).build())
				.nozzleDiameter(new BigDecimal("0.6"))
				.samplingTime(new BigDecimal("30"))
				.vacuumGaugePressure(new BigDecimal("50"))
				.finalImpingerTemperature(new BigDecimal("15"))
				.sampledDryGasVolume(new BigDecimal("1.2"))
				.collectedWaterVolume(new BigDecimal("12.5"))
				.kFactor(new BigDecimal("0.55"))
				.orificeDifferentialPressure(new BigDecimal("46.3"))
				.isokineticRatio(new BigDecimal("98.7"))
				.build())
			.build();
		SamplingSheet sheet = SamplingSheet.builder()
			.samplingPoints(List.of(point, SamplingPoint.builder().gasTemperature(new BigDecimal("101")).build()))
			.build();

		List<PointExportView> points = mapper.toSheetView(sheet).getPoints();

		assertThat(points).extracting(PointExportView::getIndex).containsExactly(1, 2);
		PointExportView first = points.getFirst();
		assertThat(first.getTs()).isEqualByComparingTo("100");
		assertThat(first.getVs()).isEqualByComparingTo("12.3");
		assertThat(first.getDensity()).isEqualByComparingTo("0.95");
		assertThat(first.getTm1()).isEqualByComparingTo("20");
		assertThat(first.getTm2()).isEqualByComparingTo("22");
		assertThat(first.getAvgTs()).isEqualByComparingTo("21");
		assertThat(first.getVm1()).isEqualByComparingTo("1.0");
		assertThat(first.getVm2()).isEqualByComparingTo("2.2");
		assertThat(first.getVacuumPressure()).isEqualByComparingTo("50");
		assertThat(first.getFinalImpingerTemperature()).isEqualByComparingTo("15");
		assertThat(first.getVm()).isEqualByComparingTo("1.2");
		assertThat(first.getVlc()).isEqualByComparingTo("12.5");
		assertThat(first.getOrificePressure()).isEqualByComparingTo("46.3");
		// 입자상 채취 정보가 없는 측정점은 해당 필드가 비어있다
		assertThat(points.get(1).getVm()).isNull();
	}

	@Test
	void 시료는_시료명_번호_공시료번호가_짧은_이름으로_노출된다() {
		SamplingSheet sheet = SamplingSheet.builder()
			.gaseousSamplings(List.of(GaseousSampling.builder()
				.sampleName("먼지-1")
				.sampleNumber("S-001")
				.blankSampleNumber("B-001")
				.samplingStartedAt(LocalTime.of(10, 0))
				.samplingEndedAt(LocalTime.of(10, 30))
				.suctionQuantity(new BigDecimal("30"))
				.gasMeterGaugePressure(new BigDecimal("13.6"))
				.inTemperature(new BigDecimal("20"))
				.outTemperature(new BigDecimal("22"))
				.beforeVolume(new BigDecimal("1.0"))
				.afterVolume(new BigDecimal("2.2"))
				.samplingVolume(new BigDecimal("1.2"))
				.build()))
			.build();

		assertThat(mapper.toSheetView(sheet).getGaseousSamplings())
			.extracting(SampleExportView::getName, SampleExportView::getNumber,
				SampleExportView::getBlankNumber, SampleExportView::getGaugePressure,
				SampleExportView::getVolumeBefore, SampleExportView::getVolumeAfter)
			.containsExactly(tuple("먼지-1", "S-001", "B-001",
				new BigDecimal("13.6"), new BigDecimal("1.0"), new BigDecimal("2.2")));
	}

	@Test
	void 데이터가_없어도_하위_뷰는_모두_존재하고_목록은_비어있다() {
		// 렌더러가 withExceptionThrower()로 동작하므로 하위 뷰가 null이면 템플릿 전체가 실패한다
		SheetExportView view = mapper.toSheetView(SamplingSheet.builder().build());

		assertThat(view.getWeather()).isNotNull();
		assertThat(view.getMoisture()).isNotNull();
		assertThat(view.getGas()).isNotNull();
		assertThat(view.getFlow()).isNotNull();
		assertThat(view.getParticle()).isNotNull();

		assertThat(view.getWeather().getTemperature()).isNull();
		assertThat(view.getMoisture().getXw()).isNull();
		assertThat(view.getFlow().getQuantity()).isNull();

		assertThat(view.getGas().getO2()).isEmpty();
		assertThat(view.getPoints()).isEmpty();
		assertThat(view.getGaseousSamplings()).isEmpty();
	}

	@Test
	void 시트_목록이_없으면_빈_목록을_반환한다() {
		assertThat(mapper.toSheetViews(null)).isEmpty();
	}
}
