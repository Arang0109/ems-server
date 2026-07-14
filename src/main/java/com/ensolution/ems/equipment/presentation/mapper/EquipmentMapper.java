package com.ensolution.ems.equipment.presentation.mapper;

import com.ensolution.ems.equipment.application.command.CreateEquipmentCommand;
import com.ensolution.ems.equipment.application.command.UpdateEquipmentCommand;
import com.ensolution.ems.equipment.domain.Equipment;
import com.ensolution.ems.equipment.domain.spec.EquipmentSpec;
import com.ensolution.ems.equipment.domain.spec.GasSamplerSpec;
import com.ensolution.ems.equipment.domain.spec.NozzleSpec;
import com.ensolution.ems.equipment.domain.spec.OtherSpec;
import com.ensolution.ems.equipment.domain.spec.ParticleSamplerSpec;
import com.ensolution.ems.equipment.domain.spec.PitotTubeSpec;
import com.ensolution.ems.equipment.presentation.request.CreateEquipmentRequest;
import com.ensolution.ems.equipment.presentation.request.UpdateEquipmentRequest;
import com.ensolution.ems.equipment.presentation.request.spec.EquipmentSpecRequest;
import com.ensolution.ems.equipment.presentation.request.spec.GasSamplerSpecRequest;
import com.ensolution.ems.equipment.presentation.request.spec.NozzleSpecRequest;
import com.ensolution.ems.equipment.presentation.request.spec.OtherSpecRequest;
import com.ensolution.ems.equipment.presentation.request.spec.ParticleSamplerSpecRequest;
import com.ensolution.ems.equipment.presentation.request.spec.PitotTubeSpecRequest;
import com.ensolution.ems.equipment.presentation.response.EquipmentResponse;
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
	CreateEquipmentCommand toCreateCommand(CreateEquipmentRequest request, Long tenantId);

	@Mapping(target = "spec", expression = "java(toDomainSpec(request.spec()))")
	UpdateEquipmentCommand toUpdateCommand(UpdateEquipmentRequest request);

	EquipmentResponse toResponse(Equipment equipment);

	List<EquipmentResponse> toResponses(List<Equipment> equipments);

	default EquipmentSpec toDomainSpec(EquipmentSpecRequest request) {
		return switch (request) {
			case null -> null;
			case ParticleSamplerSpecRequest r ->
				new ParticleSamplerSpec(r.totalVolume(), r.orificeDp(), r.yd());
			case GasSamplerSpecRequest r ->
				new GasSamplerSpec(r.totalVolume());
			case OtherSpecRequest r ->
				new OtherSpec(r.totalVolume());
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
