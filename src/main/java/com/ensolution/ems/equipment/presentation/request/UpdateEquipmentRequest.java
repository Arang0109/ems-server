package com.ensolution.ems.equipment.presentation.request;

import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.equipment.presentation.request.spec.EquipmentSpecRequest;
import com.ensolution.ems.equipment.presentation.request.spec.GasSamplerSpecRequest;
import com.ensolution.ems.equipment.presentation.request.spec.NozzleSpecRequest;
import com.ensolution.ems.equipment.presentation.request.spec.OtherSpecRequest;
import com.ensolution.ems.equipment.presentation.request.spec.ParticleSamplerSpecRequest;
import com.ensolution.ems.equipment.presentation.request.spec.PitotTubeSpecRequest;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateEquipmentRequest(
	EquipType type,

	String managementNumber,
	String serialNumber,
	String modelName,
	String equipmentName,
	String alias,

	@PositiveOrZero BigDecimal price,
	String manufacturer,
	String originCountry,
	@PastOrPresent LocalDate purchaseDate,
	String remark,

	@Positive Integer calibrationCycle,
	@PastOrPresent LocalDate lastCalibrationDate,

	@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
		property = "type"
	)
	@JsonSubTypes({
		@JsonSubTypes.Type(value = ParticleSamplerSpecRequest.class, name = "PARTICLE_SAMPLER"),
		@JsonSubTypes.Type(value = GasSamplerSpecRequest.class, name = "GAS_SAMPLER"),
		@JsonSubTypes.Type(value = PitotTubeSpecRequest.class, name = "PITOT_TUBE"),
		@JsonSubTypes.Type(value = NozzleSpecRequest.class, name = "NOZZLE"),
		@JsonSubTypes.Type(value = OtherSpecRequest.class, name = "OTHER")
	})
	EquipmentSpecRequest spec
) {}
