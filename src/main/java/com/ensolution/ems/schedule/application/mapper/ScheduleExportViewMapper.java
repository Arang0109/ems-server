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
import com.ensolution.ems.schedule.application.command.export.ScheduleExportView;
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
 */
@Component
@RequiredArgsConstructor
public class ScheduleExportViewMapper {

	private final SheetExportViewMapper sheetExportViewMapper;

	public ScheduleExportView toExportView(ScheduleSnapshot snapshot) {
		BasicInfo info = snapshot.basicInfo();
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
			.referenceNumber(info == null ? null : info.referenceNumber())
			.measurementField(info == null || info.measurementField() == null ? null : info.measurementField().getDesc())
			.schedulePurpose(info == null ? null : info.schedulePurpose())

			.sampledAt(info == null ? null : info.sampledAt())
			.receivedAt(info == null ? null : info.receivedAt())
			.analyzedAt(info == null ? null : info.analyzedAt())
			.issuedAt(info == null ? null : info.issuedAt())

			.samplingStartedAt(info == null ? null : info.samplingStartedAt())
			.samplingEndedAt(info == null ? null : info.samplingEndedAt())

			.mentor(team == null ? null : team.mentorName())
			.mentee(team == null ? null : team.menteeName())
			.facilityManager(info == null ? null : info.facilityManager())
			.samplingWitness(info == null ? null : info.samplingWitness())
			.analyst(info == null ? null : info.analyst())
			.technicalManager(info == null ? null : info.technicalManager())
			
			.tenantName(tenant == null ? null : tenant.name())
			.tenantBizNumber(tenant == null ? null : tenant.bizNumber())
			.tenantRepresentative(tenant == null ? null : tenant.representative())
			.tenantAddress(tenant == null ? null : address(tenant.roadAddress(), tenant.detailAddress()))

			.clientName(client == null ? null : client.name())
			.clientBizNumber(client == null ? null : client.bizNumber())
			.clientRepresentative(client == null ? null : client.representative())
			.clientAddress(client == null ? null : address(client.roadAddress(), client.detailAddress()))

			.workplaceName(workplace == null ? null : workplace.name())
			.workplaceBizNumber(workplace == null ? null : workplace.bizNumber())
			.workplaceAddress(workplace == null ? null : address(workplace.roadAddress(), workplace.detailAddress()))
			.workplaceGrade(workplace == null || workplace.grade() == null ? null : workplace.grade().getDesc())

			.stackName(stack == null ? null : stack.name())
			.semsNumber(stack == null ? null : stack.semsNumber())
			.businessCategory(stack == null ? null : stack.businessCategory())
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
			.particleSampler(slot(equipments, team == null ? null : team.particleSamplerId(), EquipType.PARTICLE_SAMPLER))
			.gasSampler(slot(equipments, team == null ? null : team.gasSamplerId(), EquipType.GAS_SAMPLER))
			.pitotTube(slot(equipments, team == null ? null : team.pitotTubeId(), EquipType.PITOT_TUBE))
			.nozzle(slot(equipments, team == null ? null : team.nozzleId(), EquipType.NOZZLE))
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
				.targetName(prevention.targetName())
				.removalEfficiency(prevention.removalEfficiency())
				.build());
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
}
