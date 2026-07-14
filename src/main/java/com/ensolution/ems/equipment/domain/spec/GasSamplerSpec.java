package com.ensolution.ems.equipment.domain.spec;

import java.math.BigDecimal;

public record GasSamplerSpec(
	BigDecimal totalVolume
) implements EquipmentSpec {
}
