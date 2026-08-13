package com.ensolution.ems.client_management.presentation.stack.request;

import com.ensolution.ems.global.common.enums.Grade;
import com.ensolution.ems.global.common.enums.MeasurementField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateStackRequest(
	@NotNull
	Long workplaceId,
	@NotNull(message = "측정분야 필수 선택값입니다.")
	MeasurementField field,
	@NotBlank(message = "측정시설명은 필수 입력값입니다.")
	String name,
	String semsNumber,
	Grade grade,
	String mainProduct
) {}