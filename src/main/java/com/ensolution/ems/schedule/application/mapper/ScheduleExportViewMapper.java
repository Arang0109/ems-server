package com.ensolution.ems.schedule.application.mapper;

import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.equipment.domain.InspectionItem;
import com.ensolution.ems.equipment.domain.InspectionType;
import com.ensolution.ems.equipment.domain.PitotTubeType;
import com.ensolution.ems.equipment.domain.spec.*;
import com.ensolution.ems.global.common.enums.Orientation;
import com.ensolution.ems.global.common.enums.Shape;
import com.ensolution.ems.schedule.application.command.export.EquipmentExportView;
import com.ensolution.ems.schedule.application.command.export.EquipmentInspectionExportView;
import com.ensolution.ems.schedule.application.command.export.FacilityExportView;
import com.ensolution.ems.schedule.application.command.export.PitotCoefficientExportView;
import com.ensolution.ems.schedule.application.command.export.PreventionExportView;
import com.ensolution.ems.schedule.application.command.export.SamplingItemExportView;
import com.ensolution.ems.schedule.application.command.export.ScheduleExportView;
import com.ensolution.ems.schedule.domain.Schedule;
import com.ensolution.ems.schedule.domain.snapshot.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 측정계획 스냅샷을 엑셀 템플릿용 뷰({@link ScheduleExportView})로 평탄화한다.
 * 입력값과 계산값을 모두 노출하며, 내부 도메인/스냅샷 구조와 템플릿 계약을 분리하는 경계 매퍼로서
 * 스냅샷 트리의 null 여부를 방어적으로 다룬다.
 * 측정 시트 변환은 {@link SheetExportViewMapper}에 위임한다.
 * <p>
 * 성적서 기본정보(관리번호·측정분야·측정용도와 일자 4종)는 메타에서 온다 — 문서에 사본이 없기
 * 때문이다. 두 저장소를 읽어 합치는 것은 조립부({@code ScheduleExportAssembler})가 맡고 여기서는
 * <b>순수 변환만</b> 한다 — 포트를 주입받지 않는다.
 * 실험실 입력값은 측정항목 안에 함께 들어 있어 따로 결합할 것이 없다.
 */
@Component
@RequiredArgsConstructor
public class ScheduleExportViewMapper {

	private final SheetExportViewMapper sheetExportViewMapper;

	/** 메타와 스냅샷을 합쳐 내보내기 뷰를 만든다. */
	public ScheduleExportView toExportView(Schedule meta, ScheduleSnapshot snapshot) {
		SamplingSnapshot sampling = snapshot.samplingData();
		TeamSnapshot team = snapshot.team();
		TenantSnapshot tenant = snapshot.tenant();
		ClientSnapshot client = snapshot.client();
		WorkplaceSnapshot workplace = client == null ? null : client.workplace();
		StackSnapshot stack = workplace == null ? null : workplace.stack();
		List<FacilitySnapshot> facilities = stack == null ? null : stack.facilities();
		List<PreventionSnapshot> preventions = stack == null ? null : stack.preventions();
		List<EquipmentSnapshot> equipments = snapshot.equipments();
		List<PreventionExportView> preventionViews = toPreventionViews(preventions);

		return ScheduleExportView.builder()
			.referenceNumber(meta.getReferenceNumber())
			.measurementField(meta.getMeasurementField() == null ? null : meta.getMeasurementField().getDesc())
			.schedulePurpose(meta.getSchedulePurpose())

			.sampledAt(meta.getSampledAt())
			.receivedAt(meta.getReceivedAt())
			.analyzedAt(meta.getAnalyzedAt())
			.issuedAt(meta.getIssuedAt())

			.samplingStartedAt(sampling == null ? null : sampling.samplingStartedAt())
			.samplingEndedAt(sampling == null ? null : sampling.samplingEndedAt())

			.mentorName(team == null ? null : team.mentorName())
			.menteeName(team == null ? null : team.menteeName())
			.facilityManager(sampling == null ? null : sampling.facilityManager())
			.samplingWitness(sampling == null ? null : sampling.samplingWitness())
			.analyst(tenant == null ? null : tenant.analyst())
			.technicalManager(tenant == null ? null : tenant.technicalManager())
			
			.tenantName(tenant == null ? null : tenant.name())
			.tenantBizNumber(tenant == null ? null : tenant.bizNumber())
			.tenantRepresentative(tenant == null ? null : tenant.representative())
			.tenantAddress(tenant == null ? null : address(tenant.roadAddress(), tenant.detailAddress()))

			.clientName(client == null ? null : client.name())
			.clientBizNumber(client == null ? null : client.bizNumber())
			.clientRepresentative(client == null ? null : client.representative())
			.clientRoadAddress(client == null ? null : client.roadAddress())
			.clientDetailAddress(client == null ? null : client.detailAddress())

			.workplaceName(workplace == null ? null : workplace.name())
			.workplaceBizNumber(workplace == null ? null : workplace.bizNumber())
			.businessCategory(workplace == null ? null : workplace.businessCategory())
			.workplaceRoadAddress(workplace == null ? null : workplace.roadAddress())
			.workplaceDetailAddress(workplace == null ? null : workplace.detailAddress())
			.workplaceGrade(workplace == null || workplace.grade() == null ? null : workplace.grade().getDesc())

			.stackName(stack == null ? null : stack.name())
			.semsNumber(stack == null ? null : stack.semsNumber())
			.mainProduct(stack == null ? null : stack.mainProduct())
			.stackGrade(stack == null || stack.grade() == null ? null : stack.grade().getDesc())
			.stackShape(stack == null ? null : shapeLabel(stack.shape()))
			.stackOrientation(stack == null ? null : orientationLabel(stack.orientation()))
			.horizontalLength(stack == null ? null : stack.horizontalLength())
			.verticalLength(stack == null ? null : stack.verticalLength())
			.height(stack == null ? null : stack.height())
			.standardOxygen(stack == null ? null : stack.standardOxygen())
			.facilities(toFacilityViews(facilities))
			.preventions(preventionViews)
			.items(toItemViews(snapshot.items()))
			.particleSampler(slot(equipments, EquipType.PARTICLE_SAMPLER))
			.gasSampler(slot(equipments, EquipType.GAS_SAMPLER))
			.pitotTube(slot(equipments, EquipType.PITOT_TUBE))
			.nozzle(slot(equipments, EquipType.NOZZLE))
			.equipments(toEquipmentViews(equipments))
			.sheets(sheetExportViewMapper.toSheetViews(snapshot.sheets()))
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
				.productOutput(facility.productOutput())
				.incinerationAmount(facility.incinerationAmount())
				.fuelInput(facility.fuelInput())
				.fuelType(facility.fuelType())
				.unit(facility.unit())
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
				.unit(prevention.unit())
				.targetName(prevention.targetName())
				.removalEfficiency(prevention.removalEfficiency())
				.build());
		}
		return views;
	}

	/**
	 * 측정항목 뷰 목록. <b>스냅샷에 저장된 순서를 그대로 유지한다</b> — 그 순서가 곧 성적서의 표기 순서이고,
	 * 템플릿은 {@code ${items[0].name}} 처럼 인덱스로 칸을 지목한다(기록부 한 장에 실리는 항목 수가 정해져 있다).
	 * 다른 목록과 마찬가지로 null이면 빈 리스트를 돌려준다(템플릿의 jx:each가 null에서 깨진다).
	 * <p>
	 * 실험분석정보는 {@code pollutantId}로 붙인다. <b>분석 결과가 없어도 항목은 목록에 남긴다</b> —
	 * 성적서의 칸 배치는 항목 순서가 정하므로, 미분석 항목을 빼면 뒤 항목들이 앞칸으로 밀려 버린다.
	 */
	private List<SamplingItemExportView> toItemViews(List<SamplingItemSnapshot> items) {
		if (items == null) return List.of();
		List<SamplingItemExportView> views = new ArrayList<>(items.size());
		for (SamplingItemSnapshot item : items) {
			if (item == null) continue;
			AnalysisResult analysis = item.analysis();
			views.add(SamplingItemExportView.builder()
				.name(item.nameKr())
				.nameEn(item.nameEn())
				.code(item.code())
				.cycle(item.cycle() == null ? null : item.cycle().name())
				.allowance(item.allowance())
				.oxygenApplicable(item.oxygenApplicable())
				.equipment(item.equipment())
				.testMethod(item.testMethod())
				// 아직 작성되지 않은 항목은 실험실 입력값·채취시간이 모두 null로 남는다
				.samplingStartedAt(analysis == null ? null : analysis.samplingStartedAt())
				.samplingEndedAt(analysis == null ? null : analysis.samplingEndedAt())
				.analysisValue(analysis == null ? null : analysis.analysisValue())
				.unit(analysis == null ? null : analysis.unit())
				.analysisMethod(analysis == null ? null : analysis.analysisMethod())
				.analysisEquipment(analysis == null ? null : analysis.analysisEquipment())
				.build());
		}
		return views;
	}

	/**
	 * 팀의 장비 슬롯에 해당하는 장비 뷰를 찾는다. 슬롯 id와 일치하는 장비를 우선 선택하고,
	 * 슬롯 id가 없거나 매칭되지 않으면(과거 문서 호환) 같은 유형의 첫 장비로 대체한다.
	 */
	/**
	 * 성적서의 유형별 장비 칸을 채운다. 팀 스냅샷이 유형별 슬롯을 들고 있지 않으므로 장비 목록에서
	 * 유형으로 고른다 — 어떤 유형인지는 장비 자신이 안다. 같은 유형을 두 대 이상 쓴 회차에서는
	 * 첫 번째가 대표로 들어가고, 나머지는 {@code equipments} 목록에 그대로 남는다.
	 */
	private EquipmentExportView slot(List<EquipmentSnapshot> equipments, EquipType type) {
		if (equipments == null || equipments.isEmpty()) return null;
		for (EquipmentSnapshot e : equipments) {
			if (e != null && e.type() == type) return toEquipmentView(e);
		}
		return null;
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

		List<InspectionItem> inspections = equipment.inspections() == null ? List.of() : equipment.inspections();
		InspectionItem calibration = findInspection(inspections, InspectionType.CALIBRATION);

		EquipmentExportView.EquipmentExportViewBuilder builder = EquipmentExportView.builder()
			.type(equipment.type() == null ? null : equipment.type().name())
			.typeLabel(typeLabel(equipment.type()))
			.managementNumber(equipment.managementNumber())
			.serialNumber(equipment.serialNumber())
			.modelName(equipment.modelName())
			.equipmentName(equipment.equipmentName())
			.alias(equipment.alias())
			.manufacturer(equipment.manufacturer())
			// 기존 템플릿이 참조하는 교정 프로퍼티는 CALIBRATION 항목에서 파생해 유지한다
			.calibrationCycle(calibration == null ? null : calibration.cycleMonths())
			.lastCalibrationDate(calibration == null ? null : calibration.lastInspectedAt())
			.calibrationDueDate(calibration == null ? null : calibration.nextDueDate())
			// 사양이 없거나 다른 유형이어도 목록은 비어있게 둔다(템플릿의 jx:each가 null에서 깨진다)
			.inspections(toInspectionViews(inspections))
			.coefficients(List.of())
			.nozzleDiameters(List.of());

		applySpec(builder, equipment.spec());
		return builder.build();
	}

	private InspectionItem findInspection(List<InspectionItem> inspections, InspectionType type) {
		for (InspectionItem item : inspections) {
			if (item != null && item.type() == type) return item;
		}
		return null;
	}

	private List<EquipmentInspectionExportView> toInspectionViews(List<InspectionItem> inspections) {
		List<EquipmentInspectionExportView> views = new ArrayList<>(inspections.size());
		for (InspectionItem item : inspections) {
			if (item == null || item.type() == null) continue;
			views.add(EquipmentInspectionExportView.builder()
				.type(item.type().name())
				.typeLabel(item.type().getDesc())
				.enabled(item.enabled())
				.cycleMonths(item.cycleMonths())
				.lastInspectedAt(item.lastInspectedAt())
				.nextDueDate(item.nextDueDate())
				.build());
		}
		return views;
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
			case GasAnalyzerSpec s -> builder.totalVolume(null);
			case OtherSpec s -> builder.totalVolume(null);
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

	private String shapeLabel(Shape shape) {
		if (shape == null) return null;
		return shape == Shape.CIRCULAR ? "원형" : "사각형";
	}

	private String orientationLabel(Orientation orientation) {
		if (orientation == null) return null;
		return orientation == Orientation.VERTICAL ? "수직" : "수평";
	}

	private String typeLabel(EquipType type) {
		if (type == null) return null;
		return switch (type) {
			case PARTICLE_SAMPLER -> "입자상 시료채취장비";
			case GAS_SAMPLER -> "가스상 시료채취장비";
			case GAS_ANALYZER -> "배출가스 분석기";
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
	
	private String nullCheck(String value) {
		return value == null ? "=" : value;
	}
}
