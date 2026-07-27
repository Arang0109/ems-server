package com.ensolution.ems.schedule.application.mapper;

import com.ensolution.ems.schedule.application.command.export.FlowExportView;
import com.ensolution.ems.schedule.application.command.export.GasExportView;
import com.ensolution.ems.schedule.application.command.export.MoistureExportView;
import com.ensolution.ems.schedule.application.command.export.ParticleExportView;
import com.ensolution.ems.schedule.application.command.export.PointExportView;
import com.ensolution.ems.schedule.application.command.export.SampleExportView;
import com.ensolution.ems.schedule.application.command.export.SheetExportView;
import com.ensolution.ems.schedule.application.command.export.WeatherExportView;
import com.ensolution.ems.schedule.domain.sheet.ExhaustGasData;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import com.ensolution.ems.schedule.domain.sheet.MoistureData;
import com.ensolution.ems.schedule.domain.sheet.ParticleData;
import com.ensolution.ems.schedule.domain.sheet.QuantityData;
import com.ensolution.ems.schedule.domain.sheet.Sample;
import com.ensolution.ems.schedule.domain.sheet.SamplingPoint;
import com.ensolution.ems.schedule.domain.sheet.WeatherData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 측정 시트({@link MeasurementSheet})를 엑셀 템플릿용 뷰({@link SheetExportView})로 변환한다.
 * 내부 도메인 구조와 템플릿 계약을 분리하는 경계 매퍼로서, 도메인 트리의 null 여부를 방어적으로 다룬다.
 * <p>
 * <b>하위 뷰는 소스가 null이어도 빈 인스턴스를 반환한다.</b> 렌더러가 {@code withExceptionThrower()}로
 * 동작하므로 {@code sheet.moisture}가 null이면 {@code ${sheet.moisture.ratio}} 하나에 템플릿 전체가 실패한다.
 * 목록 필드도 같은 이유로 항상 빈 리스트 이상이다(jx:each가 null에서 깨지지 않도록).
 */
@Component
public class SheetExportViewMapper {

	public List<SheetExportView> toSheetViews(List<MeasurementSheet> sheets) {
		if (sheets == null) return List.of();
		List<SheetExportView> views = new ArrayList<>(sheets.size());
		for (MeasurementSheet sheet : sheets) {
			views.add(toSheetView(sheet));
		}
		return views;
	}

	public SheetExportView toSheetView(MeasurementSheet sheet) {
		return SheetExportView.builder()
			.category(sheet.getCategory() == null ? null : sheet.getCategory().getDescription())
			.pointCount(sheet.getSamplingPointCnt())
			.weather(toWeatherView(sheet.getWeather()))
			.moisture(toMoistureView(sheet.getMoisture()))
			.gas(toGasView(sheet.getExhaustGas()))
			.flow(toFlowView(sheet.getQuantity()))
			.particle(toParticleView(sheet.getParticle(), sheet.getAvgTm()))
			.points(toPointViews(sheet.getSamplingPoints()))
			.samples(toSampleViews(sheet.getSamples()))
			.build();
	}

	private WeatherExportView toWeatherView(WeatherData weather) {
		if (weather == null) return WeatherExportView.builder().build();

		return WeatherExportView.builder()
			.pressureHpa(weather.getPressure())
			.pressureMmHg(weather.getPa())
			.condition(weather.getWeatherCondition() == null ? null : weather.getWeatherCondition().getDescription())
			.temperature(weather.getTemperature())
			.humidity(weather.getHumidity())
			.windDirection(weather.getWindDirection() == null ? null : weather.getWindDirection().getDescription())
			.windSpeed(weather.getWindSpeed())
			.build();
	}

	private MoistureExportView toMoistureView(MoistureData moisture) {
		if (moisture == null) return MoistureExportView.builder().build();

		MoistureData.BattleWeight weight = moisture.getWeight();
		MoistureData.GasMeterTemperature temperature = moisture.getGasMeterTemperature();
		MoistureData.DryGasVolume volume = moisture.getDryGasVolume();

		return MoistureExportView.builder()
			.weightBefore(weight == null ? null : weight.getBefore())
			.weightAfter(weight == null ? null : weight.getAfter())
			.inTemperature(temperature == null ? null : temperature.getIn())
			.outTemperature(temperature == null ? null : temperature.getOut())
			.volumeBefore(volume == null ? null : volume.getBefore())
			.volumeAfter(volume == null ? null : volume.getAfter())
			.suctionVelocity(moisture.getSuctionVelocity())
			.gaugePressureMmH2O(moisture.getGasMeterGaugePressure())
			.startTime(moisture.getSamplingStartTime())
			.endTime(moisture.getSamplingEndTime())

			.ratio(moisture.getXw())
			.absorbedMass(moisture.getMa())
			.avgTemperature(moisture.getTm_g())
			.dryGasVolume(moisture.getVm_g())
			.gaugePressureMmHg(moisture.getPm_g())
			.gaugePressureInchH2O(moisture.getPm_g_inch())
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

	private FlowExportView toFlowView(QuantityData quantity) {
		if (quantity == null) return FlowExportView.builder().build();

		return FlowExportView.builder()
			.area(quantity.getArea())
			.avgTemperature(quantity.getAvgTs())
			.avgTemperatureK(quantity.getAvgTg())
			.avgDynamicPressure(quantity.getAvgPv())
			.avgStaticPressure(quantity.getAvgPs())
			.density(quantity.getGasDensity())
			.pitotCoefficient(quantity.getCp())
			.velocity(quantity.getVs())
			.quantity(quantity.getQuantity())
			.standardQuantity(quantity.getStandardQuantity())
			.build();
	}

	/**
	 * 입자상 집계 뷰. 가스미터 평균 절대온도(avgTm)는 {@link ParticleData}가 아니라 시트가 직접 들고 있으나
	 * 입자상 측정점에서만 산출되는 값이므로 이 그룹에 함께 노출한다.
	 */
	private ParticleExportView toParticleView(ParticleData particle, BigDecimal avgTm) {
		if (particle == null) return ParticleExportView.builder().avgMeterTemperatureK(avgTm).build();

		return ParticleExportView.builder()
			.thimbleFilter(particle.getThimbleFilter())
			.blankThimbleFilter(particle.getBgThimbleFilter())
			.startTime(particle.getSamplingStartTime())
			.endTime(particle.getSamplingEndTime())

			.avgKFactor(particle.getAvgKFactor())
			.avgOrificePressure(particle.getAvgOrificeDp())
			.avgIsokineticRatio(particle.getAvgIsokineticRatio())
			.totalDryGasVolume(particle.getTotalVm())
			.totalSamplingTime(particle.getTotalSamplingTime())
			.avgMeterTemperatureK(avgTm)
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
		SamplingPoint.ParticleSampling particle = point.getParticle();
		SamplingPoint.ParticleSampling.EquipmentTemperature temperature =
			particle == null ? null : particle.getEquipmentTemperature();
		SamplingPoint.ParticleSampling.EquipmentVolume volume =
			particle == null ? null : particle.getEquipmentVolume();

		return PointExportView.builder()
			.index(index)
			.temperature(point.getTs())
			.dynamicPressure(point.getPv())
			.staticPressure(point.getPs())
			.velocity(point.getVs())
			.density(point.getGasDensity())

			.nozzleSize(particle == null ? null : particle.getNozzleSize())
			.samplingTime(particle == null ? null : particle.getSamplingTime())
			.vacuumPressure(particle == null ? null : particle.getVacuumGaugePressure())
			.impingerTemperature(particle == null ? null : particle.getFinalImpingerTemperature())
			.inTemperature(temperature == null ? null : temperature.getInTm())
			.outTemperature(temperature == null ? null : temperature.getOutTm())
			.volumeBefore(volume == null ? null : volume.getBeforeVm())
			.volumeAfter(volume == null ? null : volume.getAfterVm())

			.avgTemperature(temperature == null ? null : temperature.getAvgTm())
			.dryGasVolume(particle == null ? null : particle.getVm())
			.collectedWater(particle == null ? null : particle.getVlc())
			.kFactor(particle == null ? null : particle.getKFactor())
			.orificePressure(particle == null ? null : particle.getOrificeDp())
			.isokineticRatio(particle == null ? null : particle.getIsokineticRatio())
			.build();
	}

	private List<SampleExportView> toSampleViews(List<Sample> samples) {
		if (samples == null) return List.of();
		List<SampleExportView> views = new ArrayList<>(samples.size());
		for (Sample sample : samples) {
			views.add(SampleExportView.builder()
				.name(sample.getSampleName())
				.number(sample.getSampleNumber())
				.blankNumber(sample.getBlankSampleNumber())
				.startTime(sample.getStartTime())
				.endTime(sample.getEndTime())
				.suctionQuantity(sample.getSuctionQuantity())
				.gaugePressure(sample.getGasMeterGaugePressure())
				.inTemperature(sample.getInTemperature())
				.outTemperature(sample.getOutTemperature())
				.volumeBefore(sample.getBeforeVolume())
				.volumeAfter(sample.getAfterVolume())
				.samplingVolume(sample.getSamplingVolume())
				.build());
		}
		return views;
	}

	private List<BigDecimal> concentrations(List<BigDecimal> values) {
		return values == null ? List.of() : values;
	}
}
