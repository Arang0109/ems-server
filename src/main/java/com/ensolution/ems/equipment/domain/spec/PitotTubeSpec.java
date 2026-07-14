package com.ensolution.ems.equipment.domain.spec;

import com.ensolution.ems.equipment.domain.PitotTubeType;

import java.math.BigDecimal;
import java.util.List;

public record PitotTubeSpec(
	PitotTubeType pitotTubeType,
	List<PitotCoefficient> coefficients
) implements EquipmentSpec {

	public record PitotCoefficient(
		BigDecimal coefficient,
		BigDecimal velocity
	) {
	}
}
