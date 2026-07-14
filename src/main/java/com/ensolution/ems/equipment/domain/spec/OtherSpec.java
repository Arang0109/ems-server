package com.ensolution.ems.equipment.domain.spec;

import java.math.BigDecimal;

public record OtherSpec(
	BigDecimal totalVolume
) implements EquipmentSpec {
}
