package com.ensolution.ems.schedule.application.mapper;

import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.equipment.domain.PitotTubeType;
import com.ensolution.ems.equipment.domain.spec.*;
import com.ensolution.ems.global.common.enums.Shape;
import com.ensolution.ems.schedule.application.command.export.EquipmentExportView;
import com.ensolution.ems.schedule.application.command.export.FacilityExportView;
import com.ensolution.ems.schedule.application.command.export.PitotCoefficientExportView;
import com.ensolution.ems.schedule.application.command.export.PointExportView;
import com.ensolution.ems.schedule.application.command.export.PreventionExportView;
import com.ensolution.ems.schedule.application.command.export.SampleExportView;
import com.ensolution.ems.schedule.application.command.export.ScheduleExportView;
import com.ensolution.ems.schedule.application.command.export.SheetExportView;
import com.ensolution.ems.schedule.application.command.export.TargetSubstanceExportView;
import com.ensolution.ems.schedule.domain.sheet.ExhaustGasData;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import com.ensolution.ems.schedule.domain.sheet.MoistureData;
import com.ensolution.ems.schedule.domain.sheet.ParticleData;
import com.ensolution.ems.schedule.domain.sheet.QuantityData;
import com.ensolution.ems.schedule.domain.sheet.Sample;
import com.ensolution.ems.schedule.domain.sheet.SamplingPoint;
import com.ensolution.ems.schedule.domain.sheet.WeatherData;
import com.ensolution.ems.schedule.domain.snapshot.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 측정계획 스냅샷을 엑셀 템플릿용 뷰({@link ScheduleExportView})로 평탄화한다.
 * 입력값과 계산값을 모두 노출하며, 내부 도메인/스냅샷 구조와 템플릿 계약을 분리하는 경계 매퍼로서
 * 스냅샷 트리의 null 여부를 방어적으로 다룬다.
 */
@Component
public class ScheduleExportViewMapper {

	public ScheduleExportView toExportView(ScheduleSnapshot snapshot) {
		BasicInfo info = snapshot.basicInfo();
		TeamSnapshot team = snapshot.team();
		ClientSnapshot client = snapshot.client();
		WorkplaceSnapshot workplace = client == null ? null : client.workplace();
		StackSnapshot stack = workplace == null ? null : workplace.stack();
		List<FacilitySnapshot> facilities = stack == null ? null : stack.facilities();
		List<PreventionSnapshot> preventions = stack == null ? null : stack.preventions();
		List<EquipmentSnapshot> equipments = snapshot.equipments();
		List<PreventionExportView> preventionViews = toPreventionViews(preventions);

		return ScheduleExportView.builder()
			.referenceNumber(info == null ? null : info.referenceNumber())
			.mentor(team == null ? null : team.mentorName())
			.mentee(team == null ? null : team.menteeName())
			.analyst(info == null ? null : info.analyst())
			.measurementField(info == null || info.measurementField() == null ? null : info.measurementField().getDesc())
			.schedulePurpose(info == null ? null : info.schedulePurpose())
			.sampledAt(info == null ? null : info.sampledAt())
			.receivedAt(info == null ? null : info.receivedAt())
			.analyzedAt(info == null ? null : info.analyzedAt())
			.issuedAt(info == null ? null : info.issuedAt())
			.samplingStartedAt(info == null ? null : info.samplingStartedAt())
			.samplingEndedAt(info == null ? null : info.samplingEndedAt())
			.clientName(client == null ? null : client.name())
			.clientBizNumber(client == null ? null : client.bizNumber())
			.clientRepresentative(client == null ? null : client.representative())
			.clientAddress(client == null ? null : address(client.roadAddress(), client.detailAddress()))
			.facilityManager(client == null ? null : client.facilityManager())
			.samplingWitness(client == null ? null : client.samplingWitness())
			.workplaceName(workplace == null ? null : workplace.name())
			.workplaceBizNumber(workplace == null ? null : workplace.bizNumber())
			.workplaceAddress(workplace == null ? null : address(workplace.roadAddress(), workplace.detailAddress()))
			.workplaceGrade(workplace == null || workplace.grade() == null ? null : workplace.grade().getDesc())
			.stackName(stack == null ? null : stack.name())
			.businessCategory(stack == null ? null : stack.businessCategory())
			.mainProduct(stack == null ? null : stack.mainProduct())
			.stackGrade(stack == null || stack.grade() == null ? null : stack.grade().getDesc())
			.stackShape(stack == null ? null : shapeLabel(stack.shape()))
			.horizontalLength(stack == null ? null : stack.horizontalLength())
			.verticalLength(stack == null ? null : stack.verticalLength())
			.height(stack == null ? null : stack.height())
			.standardOxygen(stack == null ? null : stack.standardOxygen())
			.facilities(toFacilityViews(facilities))
			.preventions(preventionViews)
			.targetSubstances(flattenTargetSubstances(preventionViews))
			.particleSampler(slot(equipments, team == null ? null : team.particleSamplerId(), EquipType.PARTICLE_SAMPLER))
			.gasSampler(slot(equipments, team == null ? null : team.gasSamplerId(), EquipType.GAS_SAMPLER))
			.pitotTube(slot(equipments, team == null ? null : team.pitotTubeId(), EquipType.PITOT_TUBE))
			.nozzle(slot(equipments, team == null ? null : team.nozzleId(), EquipType.NOZZLE))
			.equipments(toEquipmentViews(equipments))
			.sheets(toSheetViews(snapshot.sheets()))
			.build();
	}

	private List<FacilityExportView> toFacilityViews(List<FacilitySnapshot> facilities) {
		if (facilities == null) return List.of();
		List<FacilityExportView> views = new ArrayList<>(facilities.size());
		for (FacilitySnapshot facility : facilities) {
			if (facility == null) continue;
			views.add(FacilityExportView.builder()
				.name(facility.name())
				.fuelUsage(facility.fuelUsage())
				.fuelInput(facility.fuelInput())
				.fuelType(facility.fuelType())
				.build());
		}
		return views;
	}

	private List<PreventionExportView> toPreventionViews(List<PreventionSnapshot> preventions) {
		if (preventions == null) return List.of();
		List<PreventionExportView> views = new ArrayList<>(preventions.size());
		for (PreventionSnapshot prevention : preventions) {
			if (prevention == null) continue;
			views.add(PreventionExportView.builder()
				.name(prevention.name())
				.capacity(prevention.capacity())
				.targetSubstances(toTargetSubstanceViews(prevention.name(), prevention.targetSubstances()))
				.build());
		}
		return views;
	}

	private List<TargetSubstanceExportView> toTargetSubstanceViews(
		String preventionName, List<TargetSubstanceSnapshot> targetSubstances) {
		if (targetSubstances == null) return List.of();
		List<TargetSubstanceExportView> views = new ArrayList<>(targetSubstances.size());
		for (TargetSubstanceSnapshot substance : targetSubstances) {
			if (substance == null) continue;
			views.add(TargetSubstanceExportView.builder()
				.preventionName(preventionName)
				.name(substance.name())
				.removalEfficiency(substance.removalEfficiency())
				.build());
		}
		return views;
	}

	/**
	 * 방지시설별로 중첩된 측정대상물질을 하나의 목록으로 펼친다.
	 * 물질만 한 표로 렌더링하는 템플릿이 중첩 jx:each를 쓰지 않아도 되도록 제공하며,
	 * 이미 만든 뷰 인스턴스를 그대로 재사용한다(불변이므로 공유해도 안전하다).
	 */
	private List<TargetSubstanceExportView> flattenTargetSubstances(List<PreventionExportView> preventions) {
		List<TargetSubstanceExportView> views = new ArrayList<>();
		for (PreventionExportView prevention : preventions) {
			views.addAll(prevention.getTargetSubstances());
		}
		return views;
	}

	/**
	 * 팀의 장비 슬롯에 해당하는 장비 뷰를 찾는다. 슬롯 id와 일치하는 장비를 우선 선택하고,
	 * 슬롯 id가 없거나 매칭되지 않으면(과거 문서 호환) 같은 유형의 첫 장비로 대체한다.
	 */
	private EquipmentExportView slot(List<EquipmentSnapshot> equipments, String equipmentId, EquipType type) {
		if (equipments == null || equipments.isEmpty()) return null;

		EquipmentSnapshot matched = null;
		if (equipmentId != null && !equipmentId.isBlank()) {
			for (EquipmentSnapshot e : equipments) {
				if (equipmentId.equals(e.equipmentId())) {
					matched = e;
					break;
				}
			}
		}
		if (matched == null) {
			for (EquipmentSnapshot e : equipments) {
				if (e.type() == type) {
					matched = e;
					break;
				}
			}
		}
		return toEquipmentView(matched);
	}

	private List<EquipmentExportView> toEquipmentViews(List<EquipmentSnapshot> equipments) {
		if (equipments == null) return List.of();
		List<EquipmentExportView> views = new ArrayList<>(equipments.size());
		for (EquipmentSnapshot equipment : equipments) {
			views.add(toEquipmentView(equipment));
		}
		return views;
	}

	private EquipmentExportView toEquipmentView(EquipmentSnapshot equipment) {
		if (equipment == null) return null;

		EquipmentExportView.EquipmentExportViewBuilder builder = EquipmentExportView.builder()
			.type(equipment.type() == null ? null : equipment.type().name())
			.typeLabel(typeLabel(equipment.type()))
			.managementNumber(equipment.managementNumber())
			.serialNumber(equipment.serialNumber())
			.modelName(equipment.modelName())
			.equipmentName(equipment.equipmentName())
			.alias(equipment.alias())
			.manufacturer(equipment.manufacturer())
			.calibrationCycle(equipment.calibrationCycle())
			.lastCalibrationDate(equipment.lastCalibrationDate())
			// 사양이 없거나 다른 유형이어도 목록은 비어있게 둔다(템플릿의 jx:each가 null에서 깨진다)
			.coefficients(List.of())
			.nozzleDiameters(List.of());

		applySpec(builder, equipment.spec());
		return builder.build();
	}

	/**
	 * 장비 유형별 사양을 뷰의 평탄한 필드로 옮긴다.
	 * {@link EquipmentSpec}은 sealed이므로 switch가 망라적이며, 새 사양 구현체가 추가되면 컴파일 단계에서 누락이 드러난다.
	 */
	private void applySpec(EquipmentExportView.EquipmentExportViewBuilder builder, EquipmentSpec spec) {
		if (spec == null) return;

		switch (spec) {
			case ParticleSamplerSpec s -> builder
				.totalVolume(s.totalVolume())
				.orificeDeltaH(s.orificeDp())
				.yd(s.yd());
			case GasSamplerSpec s -> builder.totalVolume(s.totalVolume());
			case OtherSpec s -> builder.totalVolume(s.totalVolume());
			case PitotTubeSpec s -> builder
				.pitotTubeType(s.pitotTubeType() == null ? null : s.pitotTubeType().name())
				.pitotTubeTypeLabel(pitotTubeTypeLabel(s.pitotTubeType()))
				.coefficients(toPitotCoefficientViews(s.coefficients()));
			case NozzleSpec s -> builder.nozzleDiameters(toNozzleDiameters(s.diameters()));
		}
	}

	private List<PitotCoefficientExportView> toPitotCoefficientViews(List<PitotTubeSpec.PitotCoefficient> coefficients) {
		if (coefficients == null) return List.of();
		List<PitotCoefficientExportView> views = new ArrayList<>(coefficients.size());
		for (PitotTubeSpec.PitotCoefficient coefficient : coefficients) {
			if (coefficient == null) continue;
			views.add(PitotCoefficientExportView.builder()
				.velocity(coefficient.velocity())
				.coefficient(coefficient.coefficient())
				.build());
		}
		return views;
	}

	private List<BigDecimal> toNozzleDiameters(List<NozzleSpec.NozzleDiameter> diameters) {
		if (diameters == null) return List.of();
		List<BigDecimal> values = new ArrayList<>(diameters.size());
		for (NozzleSpec.NozzleDiameter diameter : diameters) {
			if (diameter == null || diameter.diameter() == null) continue;
			values.add(diameter.diameter());
		}
		return values;
	}

	private List<SheetExportView> toSheetViews(List<MeasurementSheet> sheets) {
		if (sheets == null) return List.of();
		List<SheetExportView> views = new ArrayList<>(sheets.size());
		for (MeasurementSheet sheet : sheets) {
			views.add(toSheetView(sheet));
		}
		return views;
	}

	private SheetExportView toSheetView(MeasurementSheet sheet) {
		WeatherData weather = sheet.getWeather();
		MoistureData moisture = sheet.getMoisture();
		MoistureData.BattleWeight weight = moisture == null ? null : moisture.getWeight();
		MoistureData.GasMeterTemperature gmTemp = moisture == null ? null : moisture.getGasMeterTemperature();
		MoistureData.DryGasVolume dryVol = moisture == null ? null : moisture.getDryGasVolume();
		ExhaustGasData gas = sheet.getExhaustGas();
		QuantityData quantity = sheet.getQuantity();
		ParticleData particle = sheet.getParticle();

		return SheetExportView.builder()
			.category(sheet.getCategory() == null ? null : sheet.getCategory().getDescription())
			.samplingPointCount(sheet.getSamplingPointCnt())

			// 날씨
			.pressureHpa(weather == null ? null : weather.getPressure())
			.weatherCondition(weather == null || weather.getWeatherCondition() == null ? null : weather.getWeatherCondition().getDescription())
			.temperature(weather == null ? null : weather.getTemperature())
			.humidity(weather == null ? null : weather.getHumidity())
			.windDirection(weather == null || weather.getWindDirection() == null ? null : weather.getWindDirection().getDescription())
			.windSpeed(weather == null ? null : weather.getWindSpeed())
			.atmosphericPressure(weather == null ? null : weather.getPa())

			// 수분
			.moistureWeightBefore(weight == null ? null : weight.getBefore())
			.moistureWeightAfter(weight == null ? null : weight.getAfter())
			.moistureGasMeterInTemperature(gmTemp == null ? null : gmTemp.getIn())
			.moistureGasMeterOutTemperature(gmTemp == null ? null : gmTemp.getOut())
			.moistureDryGasVolumeBefore(dryVol == null ? null : dryVol.getBefore())
			.moistureDryGasVolumeAfter(dryVol == null ? null : dryVol.getAfter())
			.moistureSuctionVelocity(moisture == null ? null : moisture.getSuctionVelocity())
			.moistureGasMeterGaugePressure(moisture == null ? null : moisture.getGasMeterGaugePressure())
			.moistureRatio(moisture == null ? null : moisture.getXw())
			.moistureAbsorbedMass(moisture == null ? null : moisture.getMa())
			.moistureIntakeGasTemperature(moisture == null ? null : moisture.getTm_g())
			.moistureIntakeDryGasVolume(moisture == null ? null : moisture.getVm_g())
			.moistureGaugePressureMmHg(moisture == null ? null : moisture.getPm_g())

			// 배출가스
			.o2Concentrations(gas == null ? null : gas.getO2Concentration())
			.co2Concentrations(gas == null ? null : gas.getCo2Concentration())
			.coConcentrations(gas == null ? null : gas.getCoConcentration())
			.noxConcentrations(gas == null ? null : gas.getNoxConcentration())
			.soxConcentrations(gas == null ? null : gas.getSoxConcentration())
			.gasAnalyzerStartTime(gas == null ? null : gas.getGasAnalyzerStartTime())
			.thcAnalyzerStartTime(gas == null ? null : gas.getThcAnalyzerStartTime())
			.standardGasDensity(gas == null ? null : gas.getStandardGasDensity())
			.oxygenCorrectionFactor(gas == null ? null : gas.getO2CorrectionFactor())

			// 유량
			.area(quantity == null ? null : quantity.getArea())
			.avgGasTemperatureCelsius(quantity == null ? null : quantity.getAvgTs())
			.avgGasTemperature(quantity == null ? null : quantity.getAvgTg())
			.avgDynamicPressure(quantity == null ? null : quantity.getAvgPv())
			.avgStaticPressure(quantity == null ? null : quantity.getAvgPs())
			.gasDensity(quantity == null ? null : quantity.getGasDensity())
			.pitotCoefficient(quantity == null ? null : quantity.getCp())
			.gasVelocity(quantity == null ? null : quantity.getVs())
			.quantity(quantity == null ? null : quantity.getQuantity())
			.standardQuantity(quantity == null ? null : quantity.getStandardQuantity())
			.gasMeterAbsoluteTemperature(sheet.getAvgTm())

			// 입자상 집계
			.thimbleFilter(particle == null ? null : particle.getThimbleFilter())
			.bgThimbleFilter(particle == null ? null : particle.getBgThimbleFilter())
			.particleSamplingStartTime(particle == null ? null : particle.getSamplingStartTime())
			.particleSamplingEndTime(particle == null ? null : particle.getSamplingEndTime())
			.avgKFactor(particle == null ? null : particle.getAvgKFactor())
			.avgOrificeDp(particle == null ? null : particle.getAvgOrificeDp())
			.avgIsokineticRatio(particle == null ? null : particle.getAvgIsokineticRatio())
			.totalDryGasVolume(particle == null ? null : particle.getTotalVm())
			.totalSamplingTime(particle == null ? null : particle.getTotalSamplingTime())

			.points(toPointViews(sheet.getSamplingPoints()))
			.samples(toSampleViews(sheet.getSamples()))
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
		SamplingPoint.ParticleSampling ps = point.getParticle();
		SamplingPoint.ParticleSampling.EquipmentTemperature et = ps == null ? null : ps.getEquipmentTemperature();
		SamplingPoint.ParticleSampling.EquipmentVolume ev = ps == null ? null : ps.getEquipmentVolume();

		return PointExportView.builder()
			.index(index)
			.gasTemperature(point.getTs())
			.dynamicPressure(point.getPv())
			.staticPressure(point.getPs())
			.gasVelocity(point.getVs())
			.gasDensity(point.getGasDensity())

			.nozzleSize(ps == null ? null : ps.getNozzleSize())
			.samplingTime(ps == null ? null : ps.getSamplingTime())
			.vacuumGaugePressure(ps == null ? null : ps.getVacuumGaugePressure())
			.finalImpingerTemperature(ps == null ? null : ps.getFinalImpingerTemperature())
			.inTemperature(et == null ? null : et.getInTm())
			.outTemperature(et == null ? null : et.getOutTm())
			.dryGasVolumeBefore(ev == null ? null : ev.getBeforeVm())
			.dryGasVolumeAfter(ev == null ? null : ev.getAfterVm())

			.gasMeterAvgTemperature(et == null ? null : et.getAvgTm())
			.dryGasVolume(ps == null ? null : ps.getVm())
			.collectedWater(ps == null ? null : ps.getVlc())
			.kFactor(ps == null ? null : ps.getKFactor())
			.orificeDp(ps == null ? null : ps.getOrificeDp())
			.isokineticRatio(ps == null ? null : ps.getIsokineticRatio())
			.build();
	}

	private List<SampleExportView> toSampleViews(List<Sample> samples) {
		if (samples == null) return List.of();
		List<SampleExportView> views = new ArrayList<>(samples.size());
		for (Sample s : samples) {
			views.add(SampleExportView.builder()
				.sampleName(s.getSampleName())
				.startTime(s.getStartTime())
				.endTime(s.getEndTime())
				.suctionQuantity(s.getSuctionQuantity())
				.gasMeterGaugePressure(s.getGasMeterGaugePressure())
				.inTemperature(s.getInTemperature())
				.outTemperature(s.getOutTemperature())
				.beforeVolume(s.getBeforeVolume())
				.afterVolume(s.getAfterVolume())
				.blankSampleNumber(s.getBlankSampleNumber())
				.sampleNumber(s.getSampleNumber())
				.samplingVolume(s.getSamplingVolume())
				.build());
		}
		return views;
	}

	private String shapeLabel(Shape shape) {
		if (shape == null) return null;
		return shape == Shape.CIRCULAR ? "원형" : "사각형";
	}

	private String typeLabel(EquipType type) {
		if (type == null) return null;
		return switch (type) {
			case PARTICLE_SAMPLER -> "입자상 채취기";
			case GAS_SAMPLER -> "가스 채취기";
			case PITOT_TUBE -> "피토관";
			case NOZZLE -> "노즐";
			case OTHER -> "기타";
		};
	}

	private String pitotTubeTypeLabel(PitotTubeType type) {
		if (type == null) return null;
		return switch (type) {
			case DUST -> "먼지";
			case FINE_DUST -> "미세먼지";
			case MERCURY -> "수은";
		};
	}

	private String address(String road, String detail) {
		if (road == null && detail == null) return null;
		if (road == null) return detail;
		if (detail == null) return road;
		return road + " " + detail;
	}
}
