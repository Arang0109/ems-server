package com.ensolution.ems.equipment.presentation.mapper;

import com.ensolution.ems.equipment.application.command.CreateEquipmentCommand;
import com.ensolution.ems.equipment.application.command.UpdateEquipmentCommand;
import com.ensolution.ems.equipment.domain.Equipment;
import com.ensolution.ems.equipment.domain.InspectionItem;
import com.ensolution.ems.equipment.domain.InspectionItemChange;
import com.ensolution.ems.equipment.domain.spec.*;
import com.ensolution.ems.equipment.presentation.request.CreateEquipmentRequest;
import com.ensolution.ems.equipment.presentation.request.InspectionItemRequest;
import com.ensolution.ems.equipment.presentation.request.UpdateEquipmentRequest;
import com.ensolution.ems.equipment.presentation.request.spec.*;
import com.ensolution.ems.equipment.presentation.response.EquipmentResponse;
import com.ensolution.ems.equipment.presentation.response.InspectionItemResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder
)
public interface EquipmentMapper {

	@Mapping(target = "tenantId", source = "tenantId")
	@Mapping(target = "spec", expression = "java(toDomainSpec(request.spec()))")
	@Mapping(target = "inspections", expression = "java(toInspectionItems(request.inspections()))")
	CreateEquipmentCommand toCreateCommand(CreateEquipmentRequest request, Long tenantId);

	@Mapping(target = "spec", expression = "java(toDomainSpec(request.spec()))")
	@Mapping(target = "inspectionChanges", expression = "java(toInspectionItemChanges(request.inspections()))")
	UpdateEquipmentCommand toUpdateCommand(UpdateEquipmentRequest request);

	@Mapping(target = "inspections", expression = "java(toInspectionItemResponses(equipment.getInspections()))")
	EquipmentResponse toResponse(Equipment equipment);

	List<EquipmentResponse> toResponses(List<Equipment> equipments);

	/**
	 * 등록 시 검사 설정. null(미전달)은 그대로 넘겨 도메인이 유형별 기본 세트를 주입하게 한다.
	 * 목록에 명시한 검사는 별도 지정이 없으면 대상으로 보고 알림도 켠다.
	 */
	default List<InspectionItem> toInspectionItems(List<InspectionItemRequest> requests) {
		if (requests == null) {
			return null;
		}
		return requests.stream()
			.map(request -> new InspectionItem(
				request.type(),
				request.enabled() == null || request.enabled(),
				request.cycleMonths(),
				request.lastInspectedAt(),
				request.nextDueDateOverride(),
				request.notificationEnabled() == null || request.notificationEnabled()
			))
			.toList();
	}

	/** 수정 시 변경분. 플래그의 null을 그대로 넘겨야 도메인이 "미전달=유지"로 처리할 수 있다. */
	default List<InspectionItemChange> toInspectionItemChanges(List<InspectionItemRequest> requests) {
		if (requests == null) {
			return null;
		}
		return requests.stream()
			.map(request -> new InspectionItemChange(
				request.type(),
				request.enabled(),
				request.cycleMonths(),
				request.lastInspectedAt(),
				request.nextDueDateOverride(),
				request.notificationEnabled()
			))
			.toList();
	}

	default List<InspectionItemResponse> toInspectionItemResponses(List<InspectionItem> items) {
		if (items == null) {
			return List.of();
		}
		return items.stream()
			.map(item -> new InspectionItemResponse(
				item.type(),
				item.type().getDesc(),
				item.enabled(),
				item.cycleMonths(),
				item.lastInspectedAt(),
				item.nextDueDateOverride(),
				item.nextDueDate(),
				item.notificationEnabled()
			))
			.toList();
	}

	default EquipmentSpec toDomainSpec(EquipmentSpecRequest request) {
		return switch (request) {
			case null -> null;
			case ParticleSamplerSpecRequest r ->
				new ParticleSamplerSpec(r.totalVolume(), r.orificeDp(), r.yd());
			case GasSamplerSpecRequest r ->
				new GasSamplerSpec(r.totalVolume());
			case OtherSpecRequest r -> new OtherSpec();
			case GasAnalyzerSpecRequest r -> new GasAnalyzerSpec();
			case PitotTubeSpecRequest r ->
				new PitotTubeSpec(
					r.pitotTubeType(),
					r.coefficients() == null ? null : r.coefficients().stream()
						.map(c -> new PitotTubeSpec.PitotCoefficient(c.coefficient(), c.velocity()))
						.toList()
				);
			case NozzleSpecRequest r ->
				new NozzleSpec(
					r.diameters() == null ? null : r.diameters().stream()
						.map(d -> new NozzleSpec.NozzleDiameter(d.diameter()))
						.toList()
				);
		};
	}
}
