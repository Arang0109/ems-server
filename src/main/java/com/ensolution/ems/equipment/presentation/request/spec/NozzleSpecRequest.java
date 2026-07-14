package com.ensolution.ems.equipment.presentation.request.spec;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record NozzleSpecRequest(
	List<NozzleDiameterRequest> diameters
) implements EquipmentSpecRequest {

	public record NozzleDiameterRequest(
		@NotNull @Positive BigDecimal diameter
	) {
	}
}
