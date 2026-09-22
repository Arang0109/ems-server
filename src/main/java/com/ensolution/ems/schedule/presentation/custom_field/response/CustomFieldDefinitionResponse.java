package com.ensolution.ems.schedule.presentation.custom_field.response;

public record CustomFieldDefinitionResponse(
	Long id,
	String key,
	String label,
	Integer sortOrder
) {}
