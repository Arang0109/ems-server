package com.ensolution.ems.equipment.presentation.request.spec;

import com.ensolution.ems.equipment.domain.PitotTubeType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public record PitotTubeSpecRequest(
	@NotNull PitotTubeType pitotTubeType,
	List<PitotCoefficientRequest> coefficients
) implements EquipmentSpecRequest {

	public record PitotCoefficientRequest(
		@NotNull @PositiveOrZero BigDecimal coefficient,
		@NotNull @PositiveOrZero BigDecimal velocity
	) {
	}
}
