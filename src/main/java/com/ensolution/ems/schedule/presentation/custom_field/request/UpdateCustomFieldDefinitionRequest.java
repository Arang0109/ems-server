package com.ensolution.ems.schedule.presentation.custom_field.request;

import com.ensolution.ems.schedule.domain.custom_field.CustomFieldDefinition;
import jakarta.validation.constraints.Size;

/** 키는 바꿀 수 없어 없습니다. 비운 필드는 기존 값이 유지됩니다. */
public record UpdateCustomFieldDefinitionRequest(
	@Size(max = CustomFieldDefinition.LABEL_MAX_LENGTH)
	String label,
	Integer sortOrder
) {}
