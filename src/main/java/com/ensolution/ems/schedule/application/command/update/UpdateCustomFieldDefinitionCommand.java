package com.ensolution.ems.schedule.application.command.update;

/** 키는 등록 후 바꿀 수 없으므로 여기 없다({@code CustomFieldDefinition} javadoc). null은 유지. */
public record UpdateCustomFieldDefinitionCommand(
	String label,
	Integer sortOrder
) {}
