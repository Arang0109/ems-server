package com.ensolution.ems.equipment.presentation.request.spec;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record GasSamplerSpecRequest(
	@PositiveOrZero BigDecimal totalVolume
) implements EquipmentSpecRequest {
}
