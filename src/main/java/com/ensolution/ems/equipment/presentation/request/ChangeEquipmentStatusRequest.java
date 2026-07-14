package com.ensolution.ems.equipment.presentation.request;

import com.ensolution.ems.equipment.domain.EquipStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeEquipmentStatusRequest(
	@NotNull(message = "변경할 장비 상태는 필수 값입니다.")
	EquipStatus status
) {}
