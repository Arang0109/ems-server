package com.ensolution.ems.equipment.domain.spec;

import java.math.BigDecimal;
import java.util.List;

public record NozzleSpec(
	List<NozzleDiameter> diameters
) implements EquipmentSpec {

	public record NozzleDiameter(
		BigDecimal diameter
	) {
	}
}
