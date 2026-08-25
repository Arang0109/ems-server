package com.ensolution.ems.equipment.presentation.request;

import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.equipment.presentation.request.spec.*;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateEquipmentRequest(
	@NotNull(message = "장비 유형은 필수 선택값입니다.")
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

	/* 전달하지 않으면 장비 유형별 기본 검사 세트가 주입된다. */
	@Valid List<InspectionItemRequest> inspections,

	@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
		property = "type"
	)
	@JsonSubTypes({
		@JsonSubTypes.Type(value = ParticleSamplerSpecRequest.class, name = "PARTICLE_SAMPLER"),
		@JsonSubTypes.Type(value = GasSamplerSpecRequest.class, name = "GAS_SAMPLER"),
		@JsonSubTypes.Type(value = GasAnalyzerSpecRequest.class, name = "GAS_ANALYZER"),
		@JsonSubTypes.Type(value = PitotTubeSpecRequest.class, name = "PITOT_TUBE"),
		@JsonSubTypes.Type(value = NozzleSpecRequest.class, name = "NOZZLE"),
		@JsonSubTypes.Type(value = OtherSpecRequest.class, name = "OTHER")
	})
	EquipmentSpecRequest spec
) {}
