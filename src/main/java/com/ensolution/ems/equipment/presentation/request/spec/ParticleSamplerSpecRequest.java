package com.ensolution.ems.equipment.presentation.request.spec;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ParticleSamplerSpecRequest(
	@PositiveOrZero BigDecimal totalVolume,
	@PositiveOrZero BigDecimal orificeDp,
	@PositiveOrZero BigDecimal yd
) implements EquipmentSpecRequest {
}
