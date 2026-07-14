package com.ensolution.ems.equipment.domain.spec;

import java.math.BigDecimal;

public record ParticleSamplerSpec(
	BigDecimal totalVolume,
	BigDecimal orificeDp,
	BigDecimal yd
) implements EquipmentSpec {
}
