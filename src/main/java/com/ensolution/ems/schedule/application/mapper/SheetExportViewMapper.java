package com.ensolution.ems.schedule.application.mapper;

import com.ensolution.ems.schedule.application.command.export.FlowExportView;
import com.ensolution.ems.schedule.application.command.export.GasExportView;
import com.ensolution.ems.schedule.application.command.export.MoistureExportView;
import com.ensolution.ems.schedule.application.command.export.ParticleExportView;
import com.ensolution.ems.schedule.application.command.export.PointExportView;
import com.ensolution.ems.schedule.application.command.export.SampleExportView;
import com.ensolution.ems.schedule.application.command.export.SheetExportView;
import com.ensolution.ems.schedule.application.command.export.WeatherExportView;
import com.ensolution.ems.schedule.domain.sampling.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 측정 시트({@link SamplingSheet})를 엑셀 템플릿용 뷰({@link SheetExportView})로 변환한다.
 * 내부 도메인 구조와 템플릿 계약을 분리하는 경계 매퍼로서, 도메인 트리의 null 여부를 방어적으로 다룬다.
 * <p>
 * <b>하위 뷰는 소스가 null이어도 빈 인스턴스를 반환한다.</b> 렌더러가 {@code withExceptionThrower()}로
 * 동작하므로 {@code sheet.moisture}가 null이면 {@code ${sheet.moisture.ratio}} 하나에 템플릿 전체가 실패한다.
 * 목록 필드도 같은 이유로 항상 빈 리스트 이상이다(jx:each가 null에서 깨지지 않도록).
 */
@Component
public class SheetExportViewMapper {

	public List<SheetExportView> toSheetViews(List<SamplingSheet> sheets) {
		if (sheets == null) return List.of();
		List<SheetExportView> views = new ArrayList<>(sheets.size());
		for (SamplingSheet sheet : sheets) {
			views.add(toSheetView(sheet));
		}
		return views;
	}

	public SheetExportView toSheetView(SamplingSheet sheet) {
		return SheetExportView.builder()
			.category(sheet.getCategory() == null ? null : sheet.getCategory().getDescription())
			.samplingPointCount(sheet.getSamplingPointCount())
			.weather(toWeatherView(sheet.getWeather()))
			.moisture(toMoistureView(sheet.getMoisture()))
			.gas(toGasView(sheet.getExhaustGas()))
			.flow(toFlowView(sheet.getFlowRate()))
			.particle(toParticleView(sheet.getParticulateSampling()))
			.points(toPointViews(sheet.getSamplingPoints()))
			.gaseousSamplings(toSampleViews(sheet.getGaseousSamplings()))
			.build();
	}

	private WeatherExportView toWeatherView(WeatherData weather) {
		if (weather == null) return WeatherExportView.builder().build();

		return WeatherExportView.builder()
			.pressureHpa(weather.getAtmosphericPressure())
			.pressureMmHg(weather.getAtmosphericPressureMmHg())
			.condition(weather.getWeatherCondition() == null ? null : weather.getWeatherCondition().getDescription())
			.temperature(weather.getTemperature())
			.humidity(weather.getHumidity())
			.windDirection(weather.getWindDirection() == null ? null : weather.getWindDirection().getDescription())
			.windSpeed(weather.getWindSpeed())
			.build();
	}

	private MoistureExportView toMoistureView(MoistureData moisture) {
		if (moisture == null) return MoistureExportView.builder().build();

		MoistureData.BottleWeight weight = moisture.getBottleWeight();
		MoistureData.GasMeterTemperature temperature = moisture.getGasMeterTemperature();
		MoistureData.DryGasVolume volume = moisture.getDryGasVolume();

		return MoistureExportView.builder()
			.m1(weight == null ? null : weight.getBefore())
			.m2(weight == null ? null : weight.getAfter())
			.t1(temperature == null ? null : temperature.getIn())
			.t2(temperature == null ? null : temperature.getOut())
			.v1(volume == null ? null : volume.getBefore())
			.v2(volume == null ? null : volume.getAfter())
			.suctionVelocity(moisture.getSuctionVelocity())
			.pmMmH2O(moisture.getGasMeterGaugePressure())
			.startTime(moisture.getSamplingStartTime())
			.endTime(moisture.getSamplingEndTime())

			.xw(moisture.getMoistureRatio())
			.ma(moisture.getAbsorbedMoistureMass())
			.tmG(moisture.getAverageGasMeterTemperature())
			.vmG(moisture.getSampledDryGasVolume())
			.pmG(moisture.getGasMeterGaugePressureMmHg())
			.pmGInch(moisture.getGasMeterGaugePressureInH2O())
			.build();
	}

	private GasExportView toGasView(ExhaustGasData gas) {
		if (gas == null) return GasExportView.builder()
			.o2(List.of()).co2(List.of()).co(List.of()).nox(List.of()).sox(List.of())
			.build();

		return GasExportView.builder()
			.o2(concentrations(gas.getO2Concentration()))
			.co2(concentrations(gas.getCo2Concentration()))
			.co(concentrations(gas.getCoConcentration()))
			.nox(concentrations(gas.getNoxConcentration()))
			.sox(concentrations(gas.getSoxConcentration()))
			.analyzerStartTime(gas.getGasAnalyzerStartTime())
			.thcStartTime(gas.getThcAnalyzerStartTime())

			.standardDensity(gas.getStandardGasDensity())
			.o2CorrectionFactor(gas.getO2CorrectionFactor())
			.build();
	}

	private FlowExportView toFlowView(FlowRateData flowRate) {
		if (flowRate == null) return FlowExportView.builder().build();

		return FlowExportView.builder()
			.stackArea(flowRate.getStackArea())
			.avgTs(flowRate.getAverageGasTemperature())
			.avgTg(flowRate.getAverageGasTemperatureKelvin())
			.avgPv(flowRate.getAverageDynamicPressure())
			.avgPs(flowRate.getAverageStaticPressure())
			.gasDensity(flowRate.getGasDensity())
			.cp(flowRate.getAppliedPitotCoefficient())
			.avgVs(flowRate.getAverageGasVelocity())
			.quantity(flowRate.getWetGasFlowRate())
			.standardQuantity(flowRate.getStandardDryGasFlowRate())
			.build();
	}

	/** 입자상 집계 뷰. 가스미터 평균 절대온도까지 입자상 집계가 함께 들고 있어 그대로 옮긴다. */
	private ParticleExportView toParticleView(ParticulateSampling particle) {
		if (particle == null) return ParticleExportView.builder().build();

		return ParticleExportView.builder()
			.thimbleFilter(particle.getThimbleFilter())
			.blankThimbleFilter(particle.getBgThimbleFilter())
			.samplingStartedAt(particle.getSamplingStartedAt())
			.samplingEndedAt(particle.getSamplingEndedAt())

			.avgKFactor(particle.getAverageKFactor())
			.avgOrificePressure(particle.getAverageOrificeDifferentialPressure())
			.avgIsokineticRatio(particle.getAverageIsokineticRatio())
			.totalVm(particle.getTotalDryGasVolume())
			.totalSamplingTime(particle.getTotalSamplingTime())
			.avgTmKelvin(particle.getAverageGasMeterTemperature())
			.build();
	}

	private List<PointExportView> toPointViews(List<SamplingPoint> points) {
		if (points == null) return List.of();
		List<PointExportView> views = new ArrayList<>(points.size());
		for (int i = 0; i < points.size(); i++) {
			views.add(toPointView(i + 1, points.get(i)));
		}
		return views;
	}

	private PointExportView toPointView(int index, SamplingPoint point) {
		SamplingPoint.IsokineticSamplingData particle = point.getIsokineticSampling();
		SamplingPoint.IsokineticSamplingData.GasMeterTemperature temperature =
			particle == null ? null : particle.getGasTemperature();
		SamplingPoint.IsokineticSamplingData.GasMeterVolume volume =
			particle == null ? null : particle.getGasMeterVolume();

		return PointExportView.builder()
			.index(index)
			.ts(point.getGasTemperature())
			.pv(point.getDynamicPressure())
			.ps(point.getStaticPressure())
			.vs(point.getGasVelocity())
			.density(point.getGasDensity())

			.nozzleDiameter(particle == null ? null : particle.getNozzleDiameter())
			.samplingTime(particle == null ? null : particle.getSamplingTime())
			.vacuumPressure(particle == null ? null : particle.getVacuumGaugePressure())
			.finalImpingerTemperature(particle == null ? null : particle.getFinalImpingerTemperature())
			.tm1(temperature == null ? null : temperature.getInlet())
			.tm2(temperature == null ? null : temperature.getOutlet())
			.vm1(volume == null ? null : volume.getBefore())
			.vm2(volume == null ? null : volume.getAfter())

			.avgTs(temperature == null ? null : temperature.getAverage())
			.vm(particle == null ? null : particle.getSampledDryGasVolume())
			.vlc(particle == null ? null : particle.getCollectedWaterVolume())
			.kFactor(particle == null ? null : particle.getKFactor())
			.orificePressure(particle == null ? null : particle.getOrificeDifferentialPressure())
			.isokineticRatio(particle == null ? null : particle.getIsokineticRatio())
			.build();
	}

	private List<SampleExportView> toSampleViews(List<GaseousSampling> gaseousSamplings) {
		if (gaseousSamplings == null) return List.of();
		List<SampleExportView> views = new ArrayList<>(gaseousSamplings.size());
		for (GaseousSampling gaseousSampling : gaseousSamplings) {
			views.add(SampleExportView.builder()
				.name(gaseousSampling.getSampleName())
				.number(gaseousSampling.getSampleNumber())
				.blankNumber(gaseousSampling.getBlankSampleNumber())
				.startTime(gaseousSampling.getSamplingStartedAt())
				.endTime(gaseousSampling.getSamplingEndedAt())
				.suctionQuantity(gaseousSampling.getSuctionQuantity())
				.gaugePressure(gaseousSampling.getGasMeterGaugePressure())
				.inTemperature(gaseousSampling.getInTemperature())
				.outTemperature(gaseousSampling.getOutTemperature())
				.volumeBefore(gaseousSampling.getBeforeVolume())
				.volumeAfter(gaseousSampling.getAfterVolume())
				.samplingVolume(gaseousSampling.getSamplingVolume())
				.build());
		}
		return views;
	}

	private List<BigDecimal> concentrations(List<BigDecimal> values) {
		return values == null ? List.of() : values;
	}
}
